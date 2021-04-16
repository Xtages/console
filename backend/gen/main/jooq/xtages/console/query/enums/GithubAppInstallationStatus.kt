/*
 * This file is generated by jOOQ.
 */
package xtages.console.query.enums


import org.jooq.Catalog
import org.jooq.EnumType
import org.jooq.Schema

import xtages.console.query.Public


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
enum class GithubAppInstallationStatus(@get:JvmName("literal") val literal: String) : EnumType {
    ACTIVE("ACTIVE"),
    SUSPENDED("SUSPENDED"),
    NEW_PERMISSIONS_REQUIRED("NEW_PERMISSIONS_REQUIRED");
    override fun getCatalog(): Catalog? = schema.catalog
    override fun getSchema(): Schema = Public.PUBLIC
    override fun getName(): String = "github_app_installation_status"
    override fun getLiteral(): String = literal
}
