package com.larasoft.core.workflows;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * OSGi Configuration for Spanish Content Workflow
 */
@ObjectClassDefinition(
    name = "Spanish Content Workflow Configuration",
    description = "Configuration for Spanish content approval workflow paths and settings"
)
public @interface SpanishWorkflowConfig {

    @AttributeDefinition(
        name = "Spanish Content Path",
        description = "Root path for Spanish content that requires approval (e.g., /content/larasoft/us/es)"
    )
    String spanish_content_path() default "/content/larasoft/us/es";

    @AttributeDefinition(
        name = "Workflow Model Path",
        description = "Path to the Spanish content approval workflow model"
    )
    String workflow_model_path() default "/var/workflow/models/spanish-content-approval";
}



