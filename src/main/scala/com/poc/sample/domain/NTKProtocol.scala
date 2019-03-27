package com.poc.sample.domain

import com.poc.sample.marshaller.JsonSupport

object NTKProtocol extends JsonSupport {

  case class PortFolio(name: String, amount: Double)
  case class NewEmployee(age: Int, name: String)
  case class Employee(id:Int, age: Int, name: String)
  case class EmployeeList(employees: List[Employee])

}
