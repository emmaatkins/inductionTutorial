package controllers

import javax.inject.Inject
import models.DataModel
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}
import reactivemongo.core.errors.GenericDriverException
import repositories.DataRepository

import scala.concurrent.{ExecutionContext, Future}

class ApplicationController @Inject()(val controllerComponents: ControllerComponents, dataRepository: DataRepository, implicit val ec: ExecutionContext) extends BaseController{

    def index(): Action[AnyContent] = Action.async { implicit request =>
        dataRepository.find().map(items => Ok(Json.toJson(items)))
    }

    def create: Action[JsValue] = Action.async(parse.json) { implicit request =>
        request.body.validate[DataModel] match {
            case JsSuccess(dataModel, _) =>
                dataRepository.create(dataModel).map(_ => Created) recover {
                    case _: GenericDriverException => InternalServerError(Json.obj(
                        "message" -> "Error adding item to Mongo"
                    ))
                }
            case JsError(_) => Future(BadRequest)
        }
    }

    def read(id: String): Action[AnyContent] = Action.async { implicit request =>
        dataRepository.read(id).map{
            case Some(document) => Ok(Json.toJson(document))
            case None => NoContent
        } recover{
            case _: GenericDriverException => InternalServerError(Json.obj(
                "message" -> "Error adding item to Mongo"
            ))
        }
    }

    def update(): Action[JsValue] = Action.async(parse.json) { implicit request =>
        request.body.validate[DataModel] match {
            case JsSuccess(dataModel, _) =>
                dataRepository.update(dataModel).map{result =>
                    Accepted(Json.toJson(result))}
            case JsError(_) => Future(BadRequest)
        }
    }

    def delete(id: String): Action[AnyContent] = Action.async { implicit request =>
        dataRepository.delete(id).map(_ => Accepted) recover{
            case _: GenericDriverException => InternalServerError(Json.obj(
                "message" -> "Deletion error"))
        }
    }
}
