package com.esg.proposal.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    String action();  // e.g. "CREATE_PROPOSAL"
}
