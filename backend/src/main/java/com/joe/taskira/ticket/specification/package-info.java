/**
 * Named interface: JPA Specification builders for Ticket, reused directly by the
 * dashboard module to build its aggregate query. This is lower-quality coupling than
 * the entity/repository exposure elsewhere in ticket (a query-building internal, not
 * a real API) and is a priority candidate for replacement by a dedicated read/report
 * method once dashboard's aggregation needs are revisited; see ADR-0016.
 */
@org.springframework.modulith.NamedInterface
package com.joe.taskira.ticket.specification;
