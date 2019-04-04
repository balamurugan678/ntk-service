package com.poc.sample.domain

import java.time.LocalDateTime

object Models {

  case class BasicAuthCredentials(username: String, password: String)

  case class OAuthToken(access_token: String = java.util.UUID.randomUUID().toString,
                        token_type: String = "bearer",
                        expires_in: Int = 3600)

  case class LoggedInUser(basicAuthCredentials: BasicAuthCredentials,
                          oAuthToken: OAuthToken = new OAuthToken,
                          loggedInAt: LocalDateTime = LocalDateTime.now())

}
