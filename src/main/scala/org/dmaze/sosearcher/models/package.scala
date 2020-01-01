package org.dmaze.sosearcher

/**
  * Pure-data models.
  *
  * These contain in-process representations of the various objects
  * needed in the system.  These can be safely kept in memory.  Other
  * parts of the system take responsibility for sending them to places
  * like databases.
  *
  * These objects are generally fairly close to the Stack Exchange API
  * objects, and there are a couple of helpers here that are specific
  * to that API.  In particular they include JSON deserialization
  * helpers to create model objects from API responses.
  */
package object models {}
