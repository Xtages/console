package xtages.console.controller.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.User
import xtages.console.controller.api.model.UserInviteReq
import xtages.console.controller.model.xtagesUserWithCognitoAttributesToUser
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.query.tables.daos.XtagesUserDao
import xtages.console.service.AuthenticationService
import xtages.console.service.UserService

@Controller
class UserApiController(
    val organizationDao: OrganizationDao,
    val userDao: XtagesUserDao,
    val authenticationService: AuthenticationService,
    val userService: UserService
) : UserApiControllerBase {
    override fun getUsers(): ResponseEntity<List<User>> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val user = userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id)
        if (user!!.isOwner!!) {
            return ResponseEntity.ok(
                userService.listOrganizationUsers(organization = organization)
                    .mapNotNull(xtagesUserWithCognitoAttributesToUser::convert)
            )
        }
        return ResponseEntity(HttpStatus.FORBIDDEN)
    }

    override fun inviteUser(userInviteReq: UserInviteReq): ResponseEntity<User> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        val user = userDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId.id)
        if (user!!.isOwner!!) {
            return ResponseEntity.ok(
                xtagesUserWithCognitoAttributesToUser.convert(
                    userService.inviteUser(
                        username = userInviteReq.username,
                        name = userInviteReq.name,
                        organization = organization
                    )
                )
            )
        }
        return ResponseEntity(HttpStatus.FORBIDDEN)
    }
}
