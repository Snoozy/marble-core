function collapseFirstPost() {
}

function expandFirstPost() {
    //autosize.update($('.post-form'));
    $('.post-form').removeClass('error-border');
}

function UpdateQueryString(key, value, url) {
    if (!url) url = window.location.href;
    var re = new RegExp("([?&])" + key + "=.*?(&|#|$)(.*)", "gi"),
        hash;

    if (re.test(url)) {
        if (typeof value !== 'undefined' && value !== null)
            return url.replace(re, '$1' + key + "=" + value + '$2$3');
        else {
            hash = url.split('#');
            url = hash[0].replace(re, '$1$3').replace(/(&|\?)$/, '');
            if (typeof hash[1] !== 'undefined' && hash[1] !== null)
                url += '#' + hash[1];
            return url;
        }
    }
    else {
        if (typeof value !== 'undefined' && value !== null) {
            var separator = url.indexOf('?') !== -1 ? '&' : '?';
            hash = url.split('#');
            url = hash[0] + separator + key + '=' + value;
            if (typeof hash[1] !== 'undefined' && hash[1] !== null)
                url += '#' + hash[1];
            return url;
        }
        else
            return url;
    }
}

$(document).ready(function() {

    function htmlWithBreaks(text) {
        var htmls = [];
        var lines = text.split(/\n/);
        for (var i = 0; i < lines.length; i++) {
            htmls.push(
                jQuery(document.createElement('div')).text(lines[i]).html()
            );
        }
        return htmls.join("<br>");
    }

    $('.create-board-anchor').tooltip();

    $('.picture_upload').tooltip({'title':'Add a photo'});
    $('.comment-pic-upload').tooltip({'title':'Add a photo'});

    $(document).on('click', 'a.boomerang', function(e) {
        window.location.href = UpdateQueryString("next", encodeURIComponent(window.location.href), $(this).attr('href'));
        return false;
    });

    $(document).on('click', '.yt-embed-cover', function() {
        var id = $(this).data('id');
        $(this).replaceWith('<iframe class="yt-embed" width="500" height="281" src="https://www.youtube.com/embed/' + id + '&autoplay=1&autohide=1&border=0&wmode=opaque&enablejsapi=1" allowfullscreen></iframe>');
    });

    $('.posts-wrapper').on('click', '.post-delete', function (e) {
        if (confirm('Delete post?')) {
            var post_id = $(this).closest('.post').data('item-id');
            var $this = $(this);
            $.ajax({
                url: '/a/post/' + post_id + '/delete',
                type: 'POST',
                dataType: 'json',
                success: function () {
                    $this.closest('.post').fadeOut(500, function () {
                        $this.remove();
                    });
                    return false;
                }
            });
        }
        return false;
    });

    $('.notifications-anchor').click(function() {
        $.ajax({
            url: '/a/notifications/read',
            type: 'POST',
            success: function() {
                $('.notification-bubble').addClass('displaynone');
            }
        });
    });

    $('.single-post').on('click', '.post-delete', function(e) {
        if (confirm('Delete post?')) {
            var post_id = $(this).closest('.post').data('item-id');
            $.ajax({
                url: '/a/post/' + post_id + '/delete',
                type: 'POST',
                dataType: 'json',
                success: function () {
                    window.location.replace('/');
                    return false;
                }
            });
        }
    });

    $(document).on('click', '.action-link-repost', function (e) {
        e.preventDefault();
        var post_id = $(this).closest('.post').data('item-id');
        var post_content = $(this).closest('.post-wrapper').clone();
        post_content.find('.post-actions').remove();
        post_content.find('.hr-no-margin').remove();
        post_content.find('.comments-container').remove();
        post_content.find('.post-avatar').addClass('repost-avatar');
        post_content.find('.post-avatar').removeClass('post-avatar');
        post_content.find('.post-avatar-wrapper').addClass("modal-repost-avatar");
        post_content.find('.margin-maker').removeClass('margin-maker');
        $('#repost-modal').find('.repost-post-container').empty();
        $('#repost-modal').find('.repost-post-container').append(post_content);
        $('.modal-dialog').attr('data-post-id', post_id);
        $('#repost-modal').modal();
        $('.repost-submit-button').click(function () {
            var post_id = $(this).closest('.modal-dialog').data('post-id');
            var comment = $(this).closest('.modal-content').find('.repost-comment-input').val();
            var board = $('#repost-board-select option:selected').text();
            $.ajax({
                url: '/a/repost',
                type: 'POST',
                dataType: 'json',
                data: {
                    'repost' : post_id,
                    'comment' : comment,
                    'board' : board
                },
                success: function(response) {
                    $('#repost-modal').modal('hide');
                    $('#repost-modal').find('.repost-post-container').empty();
                    $('#repost-board-select').val([]);
                    $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter('.first-post');
                }
            });
        });

    });

    $('.posts-wrapper').on('click', '.action-link-reposted', function () {
        return false;
    });

    $(document).on('click', '.comment-delete', function(e) {
        e.preventDefault();
        if (confirm("Delete this comment?")) {
            var $comment = $(this).closest('.comment');
            var commentId = $comment.data('comment-id');
            $.ajax({
                url: '/a/comment/' + commentId + '/delete',
                type: 'POST',
                dataType: 'json',
                success: function() {
                    $comment.children('.comment-avatar').html('<img src="https://static.themarble.co/image/anon_small" class="avatar" style="height:32px;width:32px;border-radius:3px;"/>');
                    $comment.children('.commenter-anchor').remove();
                    $('<strong style="font-weight:500;margin-right:2px;margin-left:5px;" class="deleted-user">[deleted]</strong>').insertAfter($comment.children('.comment-avatar'));
                    $comment.children('.comment-content').text('[comment deleted]');
                    var c_actions = $comment.children('.comment-actions')
                    c_actions.find('.comment-options-dropdown').remove();
                    c_actions.find('.c-action.like').remove();
                    c_actions.find('.c-action.dislike').remove();
                    c_actions.find('.c-action-separator').remove();
                    c_actions.find('.vote-separator').remove();
                    c_actions.find('.like-count').css('color', '#333');
                }
            });
        }
        return false;
    });

    $('.following .board-follow-btn').click(function() {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var group_id = $(this).closest('.board-follow-btn-container').data('board-id');
        $.ajax({
            url: '/a/group/' + group_id + '/unfollow',
            type: 'POST',
            dataType: 'json',
            success: function() {
                location.reload();
            }
        });
    });

    $('.notfollowing .board-follow-btn').click(function() {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var group_id = $(this).closest('.board-follow-btn-container').data('board-id');
        $.ajax({
            url: '/a/group/' + group_id + '/follow',
            type: 'POST',
            dataType: 'json',
            success: function() {
                location.reload();
            }
        });
    });

    var numNotifs = $('.notifications-wrapper').children().length;
    var notifsRawHeight = (numNotifs * 65);
    var height = $(window).height() - 140;
    if (notifsRawHeight < height) {
        height = notifsRawHeight;
    } else if (height > 500) {
        height = 500;
    }

    $('.notification-list').css('height', height);

    $(".notifications-wrapper").slimScroll({
        height: "auto",
        wheelStep: 5
    });

    $('.conversations-scroll').slimScroll({
        height: "auto",
        wheelStep: 5
    });

    $('.conversations-container').on('click', '.conversation-wrapper', function() {
        $('.conversation-wrapper').removeClass('active');
        $(this).addClass('active');
        $(this).children('.unread-indicator').remove();
        changeConversation($(this).data('conversation-id'));
    });

    function initConversation() {
        var $msg = $('.messages-wrapper');

        $msg.slimScroll({
            height: "auto",
            wheelStep: 5,
            start: 'bottom'
        });

        $(window).resize(function() {
            $msg.slimScroll({
                height: "auto",
                wheelStep: 5,
                start: 'bottom'
            });
        });

        $('.new-message-content').on('autosize:resized', function() {
            var height = $('.message-create').outerHeight() + 34;
            $('.messages-inner').css('height', 'calc(100% - ' + height + 'px)');
            $('.messages-wrapper').slimScroll({
                height: "auto",
                wheelStep: 5,
                start: 'bottom'
            });
        });

        $('.new-message-content').keypress(function(e) {
            if (e.which == 13 && !e.shiftKey) {
                e.preventDefault();
                var $this = $(this);
                var content = $.trim($this.val());
                if (content.length > 0) {
                    var to = $this.closest('.message-create').data('to');
                    $.ajax({
                        type: 'POST',
                        url: '/a/user/' + to + '/message',
                        data: {'content': content},
                        success: function (response) {
                            $(response.item_html).hide().fadeIn(500).appendTo('.messages-wrapper');
                            $this.val('');
                            autosize.update($this);
                            $('.messages-wrapper').slimScroll({
                                scrollTo: $('.messages-wrapper')[0].scrollHeight
                            });
                            var $convo = $('.conversations-container').find('.conversation-wrapper.active');
                            $convo.find('.conversation-preview').text(response.preview);
                            if (!$convo.is(":first-child")) {
                                $convo.hide().fadeIn(500).prependTo('.conversations-scroll');
                            }
                        },
                        error: function () {
                            alert('Something went wrong...');
                        }
                    });
                }
            }
        });

        $('.messages-container').click(function() {
            $('.new-message-content').focus();
        });

        $('.new-message-content').focus();

        bindMessageScroll();

    }

    if ($('.messages-wrapper').length > 0) {
        setTimeout(function() {
            console.log('qwerty');
            var $msg = $('.messages-wrapper');
            var convoId = $msg.data('conversation-id');
            var after = $msg.children(':last').data('message-id');
            $.ajax({
                type: 'GET',
                url: '/a/conversation/' + convoId + '/poll',
                data: {'after' : after},
                cache: false,
                success: function(response) {
                    if (response.item_html) {
                        $(response.item_html).hide().fadeIn(500).appendTo('.messages-wrapper');
                        $('.conversations-container').find('.conversation-wrapper.active').find('.conversation-preview').text(response.preview);
                        $('.messages-wrapper').slimScroll({
                            scrollTo: $('.messages-wrapper')[0].scrollHeight
                        });
                    }
                }
            });
        }, 30000);
    }

    initConversation();

    $(window).resize(function() {
        var height = $(this).height() - 140;
        if (notifsRawHeight < height) {
            height = notifsRawHeight;
        } else if (height > 500) {
            height = 500;
        }
        $('.notification-list').css('height', height);
        $(".notifications-wrapper").slimScroll({
            height: "auto",
            wheelStep: 5
        });
        $('.conversations-scroll').slimScroll({
            height: "auto",
            wheelStep: 5
        });
    });


    function bindMessageScroll() {
        $('.messages-wrapper').scroll(function() {
            var $this = $(this);
            if ($this.data('done') != 'done' && $this.scrollTop() < 150) {
                var conversation = $this.data('conversation-id');
                var before = $this.children(':first').data('message-id');
                $.ajax({
                    type: 'GET',
                    url: '/a/conversation/' + conversation + '/paged',
                    data: {'before' : before},
                    success: function(response) {
                        if (response.done) {
                            $this.attr('data-done', 'done');
                        } else {
                            $(response.item_html).hide().fadeIn(500).prependTo($this);
                        }
                    }
                });
            }
        });
    }

    function changeConversation(id) {
        $.ajax({
            type: 'GET',
            url: '/a/conversation/' + id,
            success: function(response) {
                $('.messages-container').html(response.item_html);
                initConversation();
                $('.conversations-container').find('.conversation-wrapper.active').find('.conversation-preview').text(response.preview);
            }
        });
    }

    $(".navbar-notifs-wrapper .dropdown-menu").click(function(e) {
        e.stopPropagation();
    });

    $('input[placeholder], textarea[placeholder]').placeholder();

    autosize($('textarea'));

    colorVotes('post');

    colorVotes('comment');

    function getCaret(el) {
        if (el.selectionStart) {
            return el.selectionStart;
        } else if (document.selection) {
            el.focus();

            var r = document.selection.createRange();
            if (r == null) {
                return 0;
            }

            var re = el.createTextRange(),
                rc = re.duplicate();
            re.moveToBookmark(r.getBookmark());
            rc.setEndPoint('EndToStart', re);

            return rc.text.length;
        }
        return 0;
    }

    $(".navbar-search").submit(function () {
        var query = encodeURIComponent($(".search-query").val());
        if (query == "") {
            return false;
        }
        window.location.href = "/search?q=" + query;
        return false;
    });


    $('.post-form').focus(function (e) {
        e.preventDefault();
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        expandFirstPost();
    });

    $('.post-board').focus(function() {
        $(this).removeClass('error-border');
    });

    autosize($('.comment-val'));

    $(document).on('click', '.action-link-comment', function() {

        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }

        var comment_form = $(this).closest('.post-actions').siblings('.comments-container').find('.comment-form');

        $(comment_form).removeClass('displaynone');

        autosize($('.comment-val'));

        $(comment_form).find('.comment-val').focus();

        return false;

    });

    $(document).on('click', '.action-link-like', function () {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var $this = $(this);
        var post_id = $this.closest('.post').data('item-id');

        if ($this.hasClass('liked')) {
            return false;
        }

        $.ajax({
            url: '/a/post/' + post_id + '/upvote',
            type: 'POST',
            dataType: 'json',
            beforeSend: function () {
                var amount = 1;
                if ($this.parent().siblings('.dislike').children('.action-link-dislike').hasClass('disliked')) {
                    $this.parent().siblings('.dislike').children('.action-link-dislike').removeClass('disliked');
                    amount = 2;
                }
                $this.parent().siblings('.post-like-count').text(parseInt($this.parent().siblings('.post-like-count').text()) + amount);

                $this.addClass('liked');
            },
            error: function () {
            }
        });
        colorVotes('post');
        return false;
    });

    $(document).on('click', '.action-link-dislike', function () {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var $this = $(this);
        var post_id = $this.closest('.post').data('item-id');

        if ($this.hasClass('disliked')) {
            return false;
        }

        $.ajax({
            url: '/a/post/' + post_id + '/downvote',
            type: 'POST',
            dataType: 'json',
            beforeSend: function () {
                var amount = 1;
                if ($this.parent().siblings('.like').children('.action-link-like').hasClass('liked')) {
                    $this.parent().siblings('.like').children('.action-link-like').removeClass('liked');
                    amount = 2;
                }
                $this.parent().siblings('.post-like-count').text(parseInt($this.parent().siblings('.post-like-count').text()) - amount);

                $this.addClass('disliked');
            },
            error: function () {
            }
        });
        colorVotes('post');
        return false;
    });

    $(document).on('click', '.c-action.like', function () {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var $this = $(this);
        var comment_id = $(this).closest('.comment').data('comment-id');

        $.ajax({
            url: '/a/comment/' + comment_id + '/upvote',
            type: 'POST',
            dataType: 'json',
            success: function () {
                var amount = 1;
                if ($this.siblings('.c-action.dislike').hasClass('disliked')) {
                    amount = 2;
                    $this.siblings('.c-action.dislike').removeClass('disliked');
                }

                $this.addClass('liked');
                $this.siblings('.like-count').text(parseInt($this.siblings('.like-count').text()) + amount);
                colorVotes('comment');
            },
            error: function () {
            }
        });
        return false;

    });

    $(document).on('click', '.c-action.dislike', function () {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        var $this = $(this);
        var comment_id = $this.closest('.comment').data('comment-id');

        $.ajax({
            url: '/a/comment/' + comment_id + '/downvote',
            type: 'POST',
            dataType: 'json',
            success: function () {
                var amount = 1;
                if ($this.siblings('.c-action.like').hasClass('liked')) {
                    amount = 2;
                    $this.siblings('.c-action.like').removeClass('liked');
                }
                $this.addClass('disliked');
                $this.siblings('.like-count').text(parseInt($this.siblings('.like-count').text()) - amount);

                colorVotes('comment');
            }
        });
        return false;
    });

    $(document).on('click', '.c-action.reply', function(e) {
        if ($('body').hasClass('logged-out')) {
            $('#signup-modal').modal();
            return false;
        }
        e.preventDefault();
        var reply_form = $(this).closest('.comments-container').find('.comment-form').first().clone();
        reply_form.addClass('comment-reply');
        reply_form.attr('data-reply-id', $(this).closest('.comment').data('comment-id'));
        reply_form.find('.comment-val').attr('placeholder', 'Reply to comment...');
        var $comment_cont = $(this).closest('.comment-actions').siblings('.comment-children-container');
        if ($comment_cont.find('.comment-form').length == 0) {
            $comment_cont.prepend(reply_form);
        }
        $comment_cont.find('.comment-val')[0].removeAttribute('data-autosize-on');
        autosize($('textarea'));
        $comment_cont.find('.comment-val').focus();
    });


    $(document).on('keypress', '.comment-val', function(event) {
        if (event.which == 13) {
            var $this = $(this);
            var comment = $(this).val();
            var post_id = $(this).closest('.post').data('item-id');
            var $comment_form = $(this).closest('.comment-form');
            var parent = $this.closest('.comment-form').attr('data-reply-id');
            if (parent === undefined) {
                parent = 0;
            }
            if ($.trim(comment)) {
                var formdata = new FormData();
                formdata.append("data", comment);
                formdata.append("parent", parent);
                var previews = $this.closest('.comment-input').find('.comment-thumbnail-container').find('.previews');
                if (previews.children().length > 0) {
                    var id = previews.children(':first').data('upload-id');
                    formdata.append("media", comment_media_upload[id]);
                    clear_comment_media(id);
                    previews.empty();
                    previews.closest('.comment-thumbnail-container').addClass('displaynone');
                    previews.closest('.comment-input').find('.comment-val').removeClass('with-thumbs');
                }
                $.ajax({
                    url: '/a/post/' + post_id + '/comment',
                    type: 'POST',
                    dataType: 'json',
                    contentType: false,
                    processData: false,
                    data: formdata,
                    beforeSend: function() {
                        $comment_form.find('.comment-val').val('');
                    },
                    success: function (response, textStatus, jqXHR) {
                        if (parent > 0) {
                            if ($this.closest('.comment-form').siblings('.comment').length > 0) {
                                $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertBefore($this.closest('.comment-form').siblings('.comment').eq(0));
                            } else {
                                $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter($this.closest('.comment-form'));
                            }
                        } else {
                            $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter($this.closest('.comment-form').siblings('.comment-start-marker'));
                        }
                    },
                    complete: function () {
                        if ($this.closest('.comment-form').hasClass('comment-reply')) {
                            $this.closest('.comment-form').remove();
                        } else {
                            $comment_form.find('.comment-val').val('');
                            $comment_form.find('.comment-val').focus();
                        }
                    }
                });
            }
            return false;
        }

    });

    // $(document).on('blur', '.comment-reply .comment-val', function() {
    //     if (!$.trim($(this).val())) {
    //         $(this).closest('.comment-reply').remove();
    //     }
    // });

    function readURL(file, rand) {
        var reader = new FileReader();

        reader.onload = function(e) {
            $('.post-form').addClass('post-form-with-thumbnails');
            $('.thumbnail-container .previews').append($('<div class="preview" data-upload-id="' + rand + '"><button type="button" class="remove-post-image"><span class="glyphicon glyphicon-remove"></span></button><a href="' + e.target.result + '" target="_blank"><img src="' + e.target.result + '" style="height:48px;width:48px;"></a></div>'));
            $('.thumbnail-container').removeClass('displaynone');
        };

        reader.readAsDataURL(file);
    };

    $(document).on('click', '.remove-post-image', function() {
        var upload_id = $(this).closest('.preview').data('upload-id');
        delete media_form_data['media-' + upload_id];
        $(this).closest('.preview').remove();
        if ($('.thumbnail-container .previews').children().length < 1) {
            $('.thumbnail-container').addClass('displaynone');
            $('.post-form').removeClass('post-form-with-thumbnails');
        }
    });

    $(document).on('click', '.remove-comment-image', function() {
        var upload_id = $(this).closest('.comment-preview').data('upload-id');
        delete comment_media_upload[upload_id];
        if ($(this).closest('.previews').children().length < 2) {
            $(this).closest('.comment-thumbnail-container').addClass('displaynone');
            $(this).closest('.comment-input').find('.comment-text-input').find('.comment-val').removeClass('with-thumbs');
        }
        $(this).closest('.comment-preview').remove();
    });

    var media_form_data = {};

    function reset_form() {
        media_form_data = {};
    }

    $(document).on('change', '#picture_file_upload', function() {
        $(this).closest('.picture_upload').siblings('.thumbnail-container').find('.previews').empty();
        if ($(this).val() != '') {
            $.each($('#picture_file_upload')[0].files, function(i, file) {
                var rand = Math.random().toString(36).substring(2);
                media_form_data['media-' + rand] = file;
                readURL(file, rand);
            });
        }
        $(this).replaceWith($(this).clone());
    });

    var comment_media_upload = {};

    function clear_comment_media(prefix) {
        for (var key in comment_media_upload) {
            if (key.startsWith("prefix")) {
                delete comment_media_upload[key];
            }
        }
    }

    function read_comment_media_url(file, id, comment_input) {
        var reader = new FileReader();

        reader.onload = function(e) {
            comment_input.find('.form-control').addClass('with-thumbs');
            comment_input.find('.comment-thumbnail-container .previews').append($('<div class="comment-preview" data-upload-id="' + id + '"><button type="button" class="remove-comment-image"><span class="glyphicon glyphicon-remove"></span></button><a href="' + e.target.result + '" target="_blank"><img src="' + e.target.result + '" style="height:48px;width:48px;"></a></div>'))
            comment_input.find('.comment-thumbnail-container').removeClass('displaynone');
        }

        reader.readAsDataURL(file);
    }
    
    $(document).on('change', '#comment_file_upload', function() {
        $(this).closest('.comment-pic-upload').siblings('.comment-thumbnail-container').find('.previews').empty();
        if ($(this).val() != '') {
            var post_id = $(this).closest('.post').data('item-id');
            var rand = Math.random().toString(36).substring(2);
            var file = $(this)[0].files[0];
            var id = 'media-p-' + post_id + '-' + rand;
            comment_media_upload[id] = file;
            read_comment_media_url(file, id, $(this).closest('.comment-input'));
        }
        $(this).closest('.comment-text-input').find('.comment-val').focus();
        $(this).replaceWith($(this).clone());
    });

    jQuery.fn.shake = function(intShakes, intDistance, intDuration) {
        this.each(function() {
            $(this).css("position","relative");
            for (var x=1; x<=intShakes; x++) {
                $(this).animate({left:(intDistance*-1)}, (((intDuration/intShakes)/4)))
                    .animate({left:intDistance}, ((intDuration/intShakes)/2))
                    .animate({left:0}, (((intDuration/intShakes)/4)));
            }
        });
        return this;
    };

    $(document).on('click', '.chosen-single', function() {
        $(this).removeClass('error-border');
    });

    $('#repost-board-select').chosen();

    $('#post-board-select').chosen();

    $(".post-submit").click(function () {

        if ($('.post-form').val() == "" && post_tile == "") {
            $('.post-submit').shake(3, 15, 250);
            $('.post-form').addClass('error-border');
            return false;
        }

        if ($('#post-board-select option:selected').text() == "") {
            $(this).removeClass('disabled');
            $('.post-submit').shake(4, 15, 250);
            $('#post-board-select').siblings('.chosen-container').find('.chosen-single').addClass('error-border');
            return false;
        }

        var post_content = $('.post-form').val();
        var post_board = $('#post-board-select option:selected').text();
        var user = $('.post-user').val();

        if ($('.thumbnail-container .previews').children().length > 0) {
            var media_ids = null;
            var formData = new FormData();
            for (var key in media_form_data) {
                formData.append(key, media_form_data[key]);
            }
            formData.append("data", post_content);
            formData.append("board_name", post_board);
            formData.append("user", user);
            $.ajax({
                url: '/a/post',
                type: 'POST',
                dataType: 'json',
                contentType: false,
                processData: false,
                data: formData,
                beforeSend: function() {
                    $('.first-post').css('opacity', '0.6');
                },
                success: function(response) {
                    $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter('.first-post').find('a.fluidbox').fluidbox({immediateOpen: true})
                        .on('openstart', function() {$('html').addClass('noscroll');})
                        .on('closeend', function() {$('html').removeClass('noscroll');});
                    $('textarea.post-form').val('');
                    $('.post-board').val('');
                    $('.previews').empty();
                    $('.thumbnail-container').addClass('displaynone');
                    collapseFirstPost();
                    $('.first-post').css('opacity', '1');
                    reset_form();
                },
                error: function () {
                    alert("Unexpected error. Please try again later.");
                },
                complete: function () {

                }
            });
        } else {
            $('.first-post').css('opacity', '0.6');
            var formData = new FormData();
            formData.append("data", post_content);
            formData.append("board_name", post_board);
            formData.append("user", user);
            $.ajax({
                url: "/a/post",
                type: "POST",
                dataType: "json",
                contentType: false,
                processData: false,
                data: formData,
                beforeSend: function() {
                    $('.first-post').css('opacity', '0.6');
                },
                success: function (response, textStatus, jqXHR) {
                    $(response.item_html).hide().fadeIn(1000).css('display', 'block').insertAfter('.first-post');
                    $('textarea.post-form').val('');
                    collapseFirstPost();
                    $('.first-post').css('opacity', '1');
                    reset_form();
                },
                error: function () {
                    alert("Unexpected error. Please try again later.");
                }
            });
        }
        return false;
    });

    $('div[data-placeholder]').on('keydown keypress input focus', function () {
        if (this.textContent || this.childNodes.length !== 0) {
            this.dataset.divPlaceholderContent = 'true';
        }
        else {
            delete(this.dataset.divPlaceholderContent);
        }
    });

    $('.active-ph').keypress(function() {
        if ($(this).val()) {
            $(this).addClass('filled');
        } else if (!$(this).val()) {
            $(this).removeClass('filled');
        }
    });

    $(document).on('click', '.action-link-share', function() {
        var url = $(this).data('share-url');
        FB.ui({
            method: 'share',
            href: url
        }, function(response){});
        return false;
    });

    $('.fluidbox').fluidbox({
        immediateOpen: true
    }).on('openstart', function() {$('html').addClass('noscroll');})
        .on('closeend', function() {$('html').removeClass('noscroll');});

    if ($('body').hasClass('context-home')) {
        $('.context-home .posts').neverending();
    } else if ($('body').hasClass('context-user')) {
        $('.context-user ol.posts').neverending({
            context: 'user',
            entity_id: $('body').data('user-id')
        });
    } else if ($('body').hasClass('context-board')){
        $('.context-board ol.posts').neverending({
            context: 'board',
            entity_id: $('body').data('board-id')
        });
    }

    $('.msg-submit-btn').click(function() {
        var to = $(this).data('to');
        var content = $('.msg-content').val();
        $.ajax({
            type: 'POST',
            url: '/a/user/' + to + '/message',
            data: {'content' : content},
            complete: function() {
                $('#msg-modal').modal('toggle');
            }
        });
    });

    $('.user-msg').click(function() {
        $('#msg-modal').modal();
    });

});