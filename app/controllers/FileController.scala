package controllers

import java.nio.charset.StandardCharsets
import java.io.File

import akka.actor.ActorSystem
import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import org.apache.commons.io.{FileUtils, LineIterator}
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.ws.{WSClient, WSRequest, WSResponse}

import scala.concurrent.Future
import scala.util.control.Breaks._

@Singleton
class FileController @Inject()(ws: WSClient, cc: ControllerComponents, actorSystem: ActorSystem) extends AbstractController(cc) {

  def index = Action {

    val initialTime = System.nanoTime()
    val filename = "/home/anderson/big_file.txt"
    var numLines: Long = 0;
    var numThreads: Int = 5;

    val fileContents: LineIterator =
      FileUtils.lineIterator(new File(filename), StandardCharsets.UTF_8.name())

    breakable {
      while (fileContents.hasNext) {

        val executorService: ExecutorService = Executors.newFixedThreadPool(numThreads)
        val future = executorService.submit(
          new Runnable() {
            override def run(): Unit = {
              val threadId = Thread.currentThread.getId
              val request: WSRequest = ws.url("http://localhost:9000/file/show/" + numLines.toString)
                .addHttpHeaders("Accept" -> "application/json")
              val response: Future[Any] = request.get.map {
                response => {
                  System.out.println(response.json.toString())
                }
              }
              System.out.println("Added thread for task id " + numLines.toString() + " and thread id " + threadId)
            }
          }
        )
        // some operations
        val result = future.get
        if(numLines > 100000) {
          break
        }
        // println(fileContents.nextLine() + " " + numLines.toString())
        numLines += 1;
      }
    }
    println("Elapsed time: " + (System.nanoTime - initialTime) + "ns")
    Ok(Json.obj("message" -> ("Hello All Code Developers"), "linesCount" -> (numLines).toString))
  }

  def show(task_id: Int) = Action {
    val start = 100
    val end   = 3000
    val rnd = new scala.util.Random
    val time = start + rnd.nextInt( (end - start) + 1 ) // from 0.1 seconds to 3 seconds
    Thread.sleep(time)
    Ok(Json.obj("message" -> ("Response from delayed method " + time.toString() + " ms, with task id " + task_id)))
  }

}
