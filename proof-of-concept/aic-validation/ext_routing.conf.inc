<?php
/**
 * Extension routing configuration.
 *
 * This configuration defines whole workflows, or a succession of extensions,
 * based on the incoming request conditions and on other factors, such as the
 * outcome of a certain extension.
 *
 * This workflow can be altered by the 'response_router' parameter in the
 * individual extension configuration. The default behavior is to proceed with
 * the workflow defined here, but the extension router can redirect or interrupt
 * the process (e.g. to send an error code if something goes wrong).
 *
 * Extensions are referenced by the key strings in ext.conf.inc.
 *
 * This kind of configuration may be a good fit for Camel.
 */

return [
// This is a sample ingest workflow. It includes validation and ingestion.
    'validated_ingest' => [
    // These are the conditions which apply to this route. If any
    // of them returns false, the route is not applied.
    // The first, and only the first, route that satisfies all the conditions
    // is applied. This is specific to this PoC and can be different in a real
    // implementation if needed. OR logic can be optionally implemented for example.
    //
    // To make a workflow mandatory for all requests, this can be set to true.
        'conditions' => [
		// Conditions related to external requests. These are actually not
		// parsed in this PoC.
            'request' => [
            // These conditions must all be true in order for the extension
            // to be applied. OR logic can be optionally implemented.
            //
            // Keys match the ones in the request parser.
            // Values have a predefined comparator and one or more values.

                // E.g. it must be a POST, PUT or PATCH request
                'method' => ['in' => ['PATCH', 'POST', 'PUT']],

                // E.g. the request path must be equal to a certain value.
                'path' => ['eq' => '/ext:ingest'],

                // E.g. A header variable of the incoming request must be
                // within a range of specified values.
                'headers' => [
                    'content-type' => ['in' => [
                        'text/turtle', 'application/sparql-update',
                    ]],
                ],
            ],
        ],
        // Order in which the extensions are applied. This is used to build an
        // ordered list of extension names, which is then iterated by the core
        // or sent to an execution queue.
        //
        // Note that ingestion is just another step of the process; it needs
        // not be the last step or even a mandatory one.
        'extensions' => [
            'validation' => [
                // These conditions apply to the individual extensions.
                'conditions' => [
                    // A possible set of conditions is the result of a previous
                    // extension. This can be useful e.g. to set up "fallback"
                    // extensions.
                    'ext_result' => [
                        // E.g. only run this extension if 'another_ext'
                        // returns a failure.
                        'another_ext' => [
                            'result' => false,
                        ],
                    ],
                ],
            ],
            // If no options are present, set to true.
            'ingest' => true,
        ],
    ],
];
?>
