package controllers

import play.api._
import play.api.libs.iteratee.Iteratee
import play.api.libs.oauth.{OAuthCalculator, RequestToken, ConsumerKey}
import play.api.libs.ws._
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class Application extends Controller {

  val loggingIteratee = Iteratee.foreach[Array[Byte]] { array =>
    Logger.info(array.map(_.toChar).mkString)
  }

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def tweets = Action.async {
    val credentials: Option[(ConsumerKey, RequestToken)] = for {
      apiKey <- Play.configuration.getString("twitter.apiKey")
      apiSecret <- Play.configuration.getString("twitter.apiSecret")
      token <- Play.configuration.getString("twitter.token")
      tokenSecret <- Play.configuration.getString("twitter.tokenSecret")
    } yield (
        ConsumerKey(apiKey, apiSecret),
        RequestToken(token, tokenSecret)
        )

    credentials.map {
      case (consumerKey, requestToken) =>
        WS.url("https://stream.twitter.com/1.1/statuses/filter.json")
          .sign(OAuthCalculator(consumerKey, requestToken))
          .withQueryString("track" -> "reactive")
          .get { response =>
            Logger.info("Status: " + response.status)
            loggingIteratee
          }.map { _ =>
            Ok("Stream closed")
          }
    } getOrElse {
      Future {
        InternalServerError("Twitter credentials missing")
      }
    }
  }

}
