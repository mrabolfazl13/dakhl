package ir.dakhlokharj.data.local

import androidx.room.migration.Migration

/**
 * Explicit migration registry. Version 1 is the initial schema; every future version
 * must add a migration here and a corresponding test scenario. No destructive migration
 * is allowed for upgrades, so users never lose financial data.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
