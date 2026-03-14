package com.USWCicrcleLink.server.global.validation.support;

import jakarta.validation.GroupSequence;
import jakarta.validation.groups.Default;
import com.USWCicrcleLink.server.global.validation.support.ValidationGroups.*;

@GroupSequence({Default.class, NotBlankGroup.class, SizeGroup.class, PatternGroup.class,})
public interface ValidationSequence {
}
