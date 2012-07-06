package models

import play.api.db._
import anorm._
import anorm.SqlParser._
import org.apache.commons.codec.digest.DigestUtils._
import play.api.Play.current

case class Users(email: String, password: String, name: String)

object Users {
    val simple = {
        get[String]("users.email") ~
        get[String]("users.password") ~
        get[String]("users.name") map {
            case email~password~name => Users(email, password, name)
        }
    }
    
    private def passwordHash(pass: String, salt: String): String = sha256Hex(salt.padTo('0', 256) + pass)
    
    def authenticate(email: String, password: String): Option[Users] = {
        findByEmail(email).filter{ user => user.password == passwordHash(password, email) }
    }
    
    def findByEmail(email: String): Option[Users] = {
        DB.withConnection { implicit connection => 
            SQL("SELECT * FROM users WHERE users.email = {email}")
	            .on('email -> email)
	            .as(simple.singleOpt)
        }
    }
    
    def findAll(): Seq[Users] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM users").as(simple *)
        }
    }
    
    def getName(email: String): String = {
        DB.withConnection { implicit connection =>
            SQL("SELECT u.name FROM users AS u WHERE u.email = {email}")
            	.on('email -> email)
            	.as(scalar[String].single)
        }
    }
    
    def create(user: Users) {
        DB.withConnection { implicit connection =>
            SQL("INSERT INTO users (email, password, name) VALUES ({email}, {password}, {name})").on(
                    'email -> user.email,
                    'password -> passwordHash(user.password, user.email),
                    'name -> user.name
            ).executeUpdate()
        }
    }
}
