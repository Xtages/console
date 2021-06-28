package xtages.console.controller.api

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import xtages.console.controller.api.model.User
import xtages.console.controller.api.model.UserInviteReq
import xtages.console.controller.model.xtagesUserWithCognitoAttributesToUser
import xtages.console.dao.fetchOneByCognitoUserId
import xtages.console.query.tables.daos.OrganizationDao
import xtages.console.service.AuthenticationService
import xtages.console.service.UserService

@Controller
class UserApiController(
    val organizationDao: OrganizationDao,
    val authenticationService: AuthenticationService,
    val userService: UserService
) : UserApiControllerBase {
    override fun getUsers(): ResponseEntity<List<User>> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
        return ResponseEntity.ok(
            userService.listOrganizationUsers(organization = organization)
                .mapNotNull(xtagesUserWithCognitoAttributesToUser::convert)
        )
    }

    override fun inviteUser(userInviteReq: UserInviteReq): ResponseEntity<User> {
        val organization = organizationDao.fetchOneByCognitoUserId(authenticationService.currentCognitoUserId)
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
}
