/*
 * This file is generated by jOOQ.
 */
package xtages.console.query


import kotlin.collections.List

import org.jooq.Schema
import org.jooq.impl.CatalogImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class DefaultCatalog : CatalogImpl("") {
    companion object {

        /**
         * The reference instance of <code>DEFAULT_CATALOG</code>
         */
        val DEFAULT_CATALOG = DefaultCatalog()
    }

    /**
     * The schema <code>public</code>.
     */
    val PUBLIC get() = Public.PUBLIC

    override fun getSchemas(): List<Schema> = listOf(
        Public.PUBLIC
    )
}
