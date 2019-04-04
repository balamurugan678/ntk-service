package com.poc.sample.api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.Credentials
import akka.pattern.ask
import akka.util.Timeout
import com.poc.sample.domain.Models.{BasicAuthCredentials, LoggedInUser}
import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations._
import javax.ws.rs.Path
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


@Path("/ntk")
@Api(value = "/ntk", description = "Operations about NTK", produces = "application/json")
class NTKRestService(implicit elasticServiceActor: ActorRef) extends LazyLogging with Directives {

  implicit val timeout = Timeout(15.seconds)

  import com.poc.sample.domain.NTKProtocol._

  val ntkRoutes = pathPrefix("ntk") {
    ntkAPIGetRoute ~ basicAuthRoute
  }

  private val loggedInUsers = mutable.ArrayBuffer.empty[LoggedInUser]
  private val validBasicAuthCredentials = Seq(BasicAuthCredentials("admin", "admin"))

  def myUserPassAuthenticator(credentials: Credentials): Future[Option[BasicAuthCredentials]] =
    credentials match {
      case p@Credentials.Provided(id) =>
        Future {
          validBasicAuthCredentials
            .find(user => user.username == p.identifier && p.verify(user.password))
        }
      case _ => Future.successful(None)
    }

  def oAuthAuthenticator(credentials: Credentials): Option[LoggedInUser] = {
    credentials match {
      case p@Credentials.Provided(token) =>
        loggedInUsers.find(user => p.verify(user.oAuthToken.access_token)
        )
      case _ => None
    }
  }


  @Path("basic")
  @ApiOperation(value = "NTK API basic service", nickname = "NTK-basic-Service", httpMethod = "GET", produces = "application/json")
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created"),
    new ApiResponse(code = 500, message = "Internal Server Error")
  ))
  def basicAuthRoute =
    path("basic") {
      get {
        authenticateBasicAsync(realm = "basic", myUserPassAuthenticator) { user =>
          val loggedInUser = LoggedInUser(user)
          loggedInUsers.append(loggedInUser)
          complete(loggedInUser.oAuthToken)
        }
      }
    }


  @Path("employees")
  @ApiOperation(value = "NTK API Get service", nickname = "NTK-Get-Service", httpMethod = "GET", produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Authorization", dataType = "java.lang.String", paramType = "header", required = true)
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created"),
    new ApiResponse(code = 500, message = "Internal Server Error")
  ))
  def ntkAPIGetRoute =
    path("employees") {
      get {
        authenticateOAuth2(realm = "oauth", oAuthAuthenticator) { validToken =>
          complete {
            val askFutureResponse = elasticServiceActor ? "Hi"
            askFutureResponse.collect({
              case returnString: String => {
                val responseMap = parse(returnString).extract[Map[String, Any]]
                responseMap
              }
            }).recover({
              case financeInternalException: Exception => {
                throw financeInternalException
              }
            }
            )
          }
        }
      }
    }

  /*@Path("employee")
  @ApiOperation(value = "NTK API Post service", nickname = "NTK-Post-Service", httpMethod = "POST", produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "Authorization", dataType = "java.lang.String", paramType = "header", required = true),
    new ApiImplicitParam(name = "CreateEmployee", dataType = "com.poc.sample.domain.NTKProtocol$NewEmployee", paramType = "body", required = true)
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created"),
    new ApiResponse(code = 500, message = "Internal Server Error")
  ))
  def ntkAPIPostRoute =
    path("employee") {
      post {
        authenticateOAuth2(realm = "oauth", oAuthAuthenticator) { validToken =>
          entity(as[NewEmployee]) {
            employee =>
              complete {
                val conn: Connection = getJDBCConnection
                val insertSql =
                  """
                    |insert into employee (age,name)
                    |values (?,?)
                  """.stripMargin
                val preparedStmt: PreparedStatement = conn.prepareStatement(insertSql)
                preparedStmt.setInt(1, employee.age)
                preparedStmt.setString(2, employee.name)
                preparedStmt.execute
                preparedStmt.close()
                logger.info("Employee has been created with name" + employee.name)
                NewEmployee(employee.age, employee.name)
              }
          }
        }

      }
    }*/



}
