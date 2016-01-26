$(function() {

    var defaults = {
        type : 'post',
        context : 'home',
        spinner_selector : '#neverending_spinner',
        entity_id : 0
    };

    $.fn.neverending = function(options) {

        options = $.extend({}, defaults, options || {});
        var scrolled = false,
            more = true,
            $this = $(this),
            _object = $this[0];
        //Scrolled is so not much work is done in window.scroll(). See: http://ejohn.org/blog/learning-from-twitter/

        $(window).scroll(function() {
            if (more) {
                scrolled = true;
            }
        });

        setInterval(function() {
            if (scrolled) {
                scrolled = false;

                if ($(window).scrollTop() >= $(document).height() - $(window).height() - 800) {

                    var last_post_id = $(_object.lastElementChild).data('item-id');

                    if (options.context === "home") {
                        $.ajax({
                            url: '/a/neverending',
                            type: 'GET',
                            dataType: 'json',
                            data: {
                                'type': options.type,
                                'context': options.context,
                                'after': last_post_id
                            },
                            async: false,
                            success: function (response) {
                                $this.append(response.item_html).find('a.fluidbox').fluidbox({
                                    immediateOpen: true
                                }).on('openstart', function() {$('html').addClass('noscroll');})
                                    .on('closeend', function() {$('html').removeClass('noscroll');});

                                if (response.item_html === "") {
                                    more = false;
                                    $(options.spinner_selector).hide();
                                }

                                $('.like-count').each(function () {
                                    if (parseInt($(this).text(), 10) > 0) {
                                        $(this).css('color', '#009900');
                                    } else if (parseInt($(this).text(), 10) < 0) {
                                        $(this).css('color', '#CC1100');
                                    }
                                });

                            }
                        });
                    } else if (options.context === 'user' && options.entity_id !== 0) {
                        $.ajax({
                            url: '/a/neverending',
                            type: 'GET',
                            dataType: 'json',
                            data: {
                                'type': options.type,
                                'context': options.context,
                                'after': last_post_id,
                                'user': options.entity_id
                            },
                            async: false,
                            success: function (response) {
                                $this.append(response.item_html).find('a.fluidbox').fluidbox({
                                    immediateOpen: true
                                }).on('openstart', function() {$('html').addClass('noscroll');})
                                    .on('closeend', function() {$('html').removeClass('noscroll');});

                                if (response.item_html === "") {
                                    more = false;
                                    $(options.spinner_selector).hide();
                                }

                                $('.like-count').each(function () {
                                    if (parseInt($(this).text(), 10) > 0) {
                                        $(this).css('color', '#009900');
                                    } else if (parseInt($(this).text(), 10) < 0) {
                                        $(this).css('color', '#CC1100');
                                    }
                                });

                            }
                        });
                    } else if (options.context === "board" && options.entity_id !== 0) {
                        $.ajax({
                            url: '/a/neverending',
                            type: 'GET',
                            dataType: 'json',
                            data: {
                                'type': options.type,
                                'context': options.context,
                                'after': last_post_id,
                                'board': options.entity_id
                            },
                            async: false,
                            success: function (response) {
                                $this.append(response.item_html).find('a.fluidbox').fluidbox({
                                    immediateOpen: true
                                }).on('openstart', function() {$('html').addClass('noscroll');})
                                    .on('closeend', function() {$('html').removeClass('noscroll');});

                                if (response.item_html === "") {
                                    more = false;
                                    $(options.spinner_selector).hide();
                                }

                                $('.like-count').each(function () {
                                    if (parseInt($(this).text(), 10) > 0) {
                                        $(this).css('color', '#009900');
                                    } else if (parseInt($(this).text(), 10) < 0) {
                                        $(this).css('color', '#CC1100');
                                    }
                                });

                            }
                        });
                    }
                }

            }

        }, 200);

    }
});