<?php
/**
 * Extension configuration file.
 * This can be a YAML file, RDF data on a flat file or in a triplestore, XML,
 * or any other non-executable code.
 *
 * This file configures several aspects of each extension:
 * 1. Whether the extension is active.
 * 2. Service bindings. Each extension defines one and only one service.
 * 3. Actions to be taken based on response from the service.
 */
return [
    'validation' => [
        'active' => true,

        // service called by this extension.
        // These fields are general and are parsed by the Core. They are not
        // related to the specific service, for which a separate configuration
        // (in this example the actual validataion rules) is used.
        'service' => [
            // Endpoint URI. This can be replaced by a discovery service.
            'uri' => getenv('SVC_ENDPOINT'),
            // HTTP method to use for the call.
            'method' => 'POST',
            // Map of request parameters to send to the service.
            // Keys are the field names (form data or GET parameters) of the
            // incoming request, values are the field names of the request sent
            // to the service. 
            'fields' => [
                'content' => 'content',
            ],
            // Determine action to take based on response from service.
            // In this case it is based on HTTP return codes but it can be
            // adapted to more complex requirements.
            'response_router' => [
                '204' => 'forward',
                '412' => 'send_error',
            ]
        ],
    ],

    // Fill the blanks here.
    'provenance' => [
        'active' => true,
    ],

    // This is the ingestion step, usually but not necessarily the last one.
    // It is treated as any other service. It can be optional in certain cases.
    'ingest' => [
        'active' => true,
        'service' => [
            'uri' => getenv('REPO_ENDPOINT'),
            'method' => 'POST',
            'fields' => [
                'content' => 'content',
            ],
            // An asterisk sign matches all request codes not matched by
            // previous keys in the list.
            'response_router' => [
                '*' => 'forward',
            ]
        ],
    ]
];
