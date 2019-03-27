package com.poc.sample.api

import java.sql.{Connection, PreparedStatement, ResultSet, Statement}

import akka.http.scaladsl.server.Directives
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import io.swagger.annotations._
import javax.ws.rs.Path
import org.h2.jdbcx.JdbcDataSource

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

@Path("/ntk")
@Api(value = "/ntk", description = "Operations about NTK", produces = "application/json")
class NTKRestService extends LazyLogging with Directives {

  implicit val timeout = Timeout(15.seconds)

  import com.poc.sample.domain.NTKProtocol._

  val ntkRoutes = pathPrefix("ntk") {
    ntkAPIPostRoute ~ ntkAPIGetRoute
  }

  @ApiOperation(value = "NTK API Get service", nickname = "NTK-Get-Service", httpMethod = "GET", produces = "application/json")
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created"),
    new ApiResponse(code = 500, message = "Internal Server Error")
  ))
  def ntkAPIGetRoute =
    get {
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

  @ApiOperation(value = "NTK API Post service", nickname = "NTK-Post-Service", httpMethod = "POST", produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "CreateEmployee", dataType = "com.poc.sample.domain.NTKProtocol$NewEmployee", paramType = "body", required = true)
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created"),
    new ApiResponse(code = 500, message = "Internal Server Error")
  ))
  def ntkAPIPostRoute =
    post {
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

  private def getJDBCConnection = {
    val ds = new JdbcDataSource()
    ds.setURL("jdbc:h2:~/test")
    ds.setUser("sa")
    ds.setPassword("")
    ds.getConnection()
  }
}
