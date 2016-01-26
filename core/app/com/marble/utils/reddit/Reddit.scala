package com.marble.utils.reddit

import com.github.jreddit.retrieval.Submissions
import com.github.jreddit.retrieval.params.SubmissionSort
import com.github.jreddit.utils.restclient.{HttpRestClient, RestClient}
import scala.collection.JavaConverters._

object Reddit {

    val rest: RestClient = new HttpRestClient()
    val subms: Submissions = new Submissions(rest)

    def getSubredditPosts(sub: String, count: Int = 10) = {
        subms.ofSubreddit(sub, SubmissionSort.TOP, -1, count, null, null, true).asScala.toList.filter(s => !s.isStickied && !s.isSelf)
    }
}
