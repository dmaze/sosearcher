package org.dmaze.sosearcher.actors

/**
  * The priority of a fetch request.
  *
  * The Stack Exchange API limits request rates, but encourages batched
  * requests.  The priority of a fetch request determines how quickly we
  * will execute a request, and in which order.
  *
  * If there are requests with [[UserPriority]] they will execute ahead
  * of requests with [[AutoPriority]], and there will be a relatively
  * short timeout to execute them.  [[AutoPriority]] is the default
  * priority.
  */
sealed trait Priority

/**
  * Fetch request priority for user-initiated requests.
  *
  * This priority corresponds to requests directly initiated by users,
  * for example by entering a question URL into a form.  These requests
  * execute ahead of [[AutoPriority]] requests and can run with a shorter
  * timeout.
  */
case object UserPriority extends Priority

/**
  * Default fetch request priority.
  *
  * This priority is used for scraping requests and other similar background
  * tasks.  There may be a longer delay to batch up these requests.  Requests
  * with [[UserPriority]] will preempt these.
  */
case object AutoPriority extends Priority
