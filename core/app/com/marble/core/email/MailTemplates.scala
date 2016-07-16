package com.marble.core.email

import com.marble.utils.Etc
import scala.concurrent.ExecutionContext.Implicits.global

object MailTemplates {

    def sendWelcomeEmail(firstName: String, email: String) = {
        val sendEmail = Email(
            subject = "Welcome to Marble!",
            from = EmailAddress("Marble", "info@themarble.co"),
            text = welcomeEmailTextGen(firstName),
            htmlText = com.marble.core.web.views.html.email.welcome(firstName).toString()
        ).to(firstName, email)
        AsyncMailer.sendEmail(sendEmail)
    }

    def welcomeEmailTextGen(name: String): String = {
        s"""Hey, $name \n Now that you have secured your spot on Marble (nice!), you're ready to start
            |exploring boards. \n\n What's a board? Boards are places for anyone to talk about anything. For example, you can: \n\n
            |1. Talk about the latest sports game. \n
            |2. Marvel at the latest high tech. \n
            |3. Weigh in on the next election. \n
            |4. See which movie to watch next. \n\n
            |Hope to see you around!
            |\n\n\n
            |Facebook: https://www.facebook.com/CilloHQ
            |Twitter: https://www.twitter.com/CilloHQ
            |Google: https://plus.google.com/+CilloHQ
            |Blog: http://blog.marble.co
         """.stripMargin
    }

}