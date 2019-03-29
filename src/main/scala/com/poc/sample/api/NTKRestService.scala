package com.poc.sample.api

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}
import java.time.LocalDateTime

import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.directives.Credentials
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations._
import javax.ws.rs.Path
import org.h2.jdbcx.JdbcDataSource

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

@Path("/ntk")
@Api(value = "/ntk", description = "Operations about NTK", produces = "application/json")
class NTKRestService extends LazyLogging with Directives {

  implicit val timeout = Timeout(15.seconds)

  import com.poc.sample.domain.NTKProtocol._

  val ntkRoutes = pathPrefix("ntk") {
    ntkAPIPostRoute ~ ntkAPIGetRoute ~ basicAuthRoute
  }

  case class BasicAuthCredentials(username: String, password: String)

  private val loggedInUsers = mutable.ArrayBuffer.empty[LoggedInUser]
  private val validBasicAuthCredentials = Seq(BasicAuthCredentials("admin", "admin"))

  case class OAuthToken(access_token: String = java.util.UUID.randomUUID().toString,
                        token_type: String = "bearer",
                        expires_in: Int = 3600)

  case class LoggedInUser(basicAuthCredentials: BasicAuthCredentials,
                          oAuthToken: OAuthToken = new OAuthToken,
                          loggedInAt: LocalDateTime = LocalDateTime.now())

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
            val conn: Connection = getJDBCConnection
            val stmt = conn.createStatement
            val sql = "select * from employee"
            val resultSet: ResultSet = stmt.executeQuery(sql)
            val employeeList = new ListBuffer[Employee]()
            while (resultSet.next) {
              val id = resultSet.getInt("id")
              val age = resultSet.getInt("age")
              val name = resultSet.getString("name")
              val employee = Employee(id, age, name)
              employeeList += employee
            }
            logger.info("Employee table has been queried with total count" + employeeList.size)
            employeeList
          }
        }
      }
    }

  @Path("employee")
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
    }


  private def getJDBCConnection = {
    val ds = new JdbcDataSource()
    ds.setURL("jdbc:h2:~/test")
    ds.setUser("sa")
    ds.setPassword("")
    ds.getConnection()
  }
}
