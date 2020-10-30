package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import models.DataModel
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import reactivemongo.api.commands.{LastError, WriteResult}
import reactivemongo.core.errors.GenericDriverException
import repositories.DataRepository
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  val controllerComponents: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val mockDataRepository: DataRepository = mock[DataRepository]
  implicit val system: ActorSystem = ActorSystem("Sys")
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  object TestApplicationController extends ApplicationController(
    controllerComponents, mockDataRepository, executionContext
  )
  val dataModel: DataModel = DataModel(
    "abcd",
    "test name",
    "test description",
    100
  )

  val jsonBody: JsObject =
    Json.obj(
      "_id" -> "abcd",
      "name" -> "test name",
      "description" -> "test description",
      "numSales" -> 100
    )

  "ApplicationController .index()" should {

    val jsonBody: JsArray = Json.arr(
      Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      ))

    when(mockDataRepository.find(any())(any()))
      .thenReturn(Future(List(dataModel)))

    val result = TestApplicationController.index()(FakeRequest())

    "return OK" in {
      status(result) shouldBe Status.OK
    }

    "return the correct JSON" in {
      await(jsonBodyOf(result)) shouldBe jsonBody
    }
  }

  "ApplicationController .create" when {
    "the json body is valid" should {

      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      val writeResult: WriteResult = LastError(ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None)

      when(mockDataRepository.create(any()))
        .thenReturn(Future(writeResult))

      val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

      "return CREATED (201)" in {
        status(result) shouldBe Status.CREATED
      }
    }

    "the json body is not valid" should {
      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "stuff" -> "wrong stuff"
      )

      val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

      "return BAD_REQUEST" in {
        status(result) shouldBe Status.BAD_REQUEST
      }
    }

    "the mongo data creation failed" should {

      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "name" -> "test name",
        "description" -> "test description",
        "numSales" -> 100
      )

      when(mockDataRepository.create(any()))
        .thenReturn(Future.failed(GenericDriverException("Error")))

      "return an error" in {

        val result = TestApplicationController.create()(FakeRequest().withBody(jsonBody))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        await(bodyOf(result)) shouldBe Json.obj("message" -> "Error adding item to Mongo").toString()
      }
    }
}

  "ApplicationController .read(id: String)" when {
    val jsonBody: JsObject = Json.obj(
      "_id" -> "abcd",
      "name" -> "test name",
      "description" -> "test description",
      "numSales" -> 100
    )


    "id is valid" should {
      when(mockDataRepository.read(any())).thenReturn(Future(dataModel))
      val result = TestApplicationController.read("abcd")(FakeRequest())

      "return the correct json" in {
        await(jsonBodyOf(result)) shouldBe jsonBody
      }
      "return OK" in {
        status(result) shouldBe Status.OK
      }
    }

    "read failed" should {
      when(mockDataRepository.read(any())).thenReturn(Future.failed(GenericDriverException("Error")))

      "return an error" in{
        val result = TestApplicationController.read("abcd")(FakeRequest())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        await(jsonBodyOf(result)) shouldBe Json.obj("message" -> "Error adding item to Mongo")
      }
    }

  }

  "ApplicationController .update()" when {
    "json body is valid" should {


      "return the correct JSON" in {

        when(mockDataRepository.update(any()))
          .thenReturn(Future.successful(dataModel))

        val result = TestApplicationController.update()(FakeRequest().withBody(jsonBody))

        await(jsonBodyOf(result)) shouldBe jsonBody
      }
    }

    "json body is not valid" should {
      val jsonBody: JsObject = Json.obj(
        "_id" -> "abcd",
        "stuff" -> "wrong stuff"
      )

      val result = TestApplicationController.update()(FakeRequest().withBody(jsonBody))

      "return BAD_REQUEST" in {
        status(result) shouldBe Status.BAD_REQUEST
      }
    }
  }

    "ApplicationController .delete(id: String)" when {
      val writeResult: WriteResult = LastError(ok = true, None, None, None, 0, None, updatedExisting = false, None, None, wtimeout = false, None, None)

      when(mockDataRepository.delete(any())).thenReturn(Future(writeResult))

      val result = TestApplicationController.delete("abcd")(FakeRequest())
      "id is valid" should {

        "return ACCEPTED (202)" in {
          status(result) shouldBe Status.ACCEPTED
        }
      }
      "delete failed" should {
        when(mockDataRepository.delete(any())).thenReturn(Future.failed(GenericDriverException("Error")))
        val result = TestApplicationController.delete("abcd")(FakeRequest())

        "return an error" in{
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          await(jsonBodyOf(result)) shouldBe Json.obj("message" -> "Deletion error")
        }
      }

      }
}
