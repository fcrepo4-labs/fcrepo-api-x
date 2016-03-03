<?php
/**
 * API Core class.
 * Parses incoming requests and routes them to the appropriate extensions.
 */

$m = new Core;
$m->main();


class Core
{

    private $_ext_route_conf;
    private $_ext_conf;

    function __construct()
    {
        // Extension configuration. This can be stored in an external file or
        // document store of any sort.
        $this->_ext_route_conf = include_once
            $_SERVER['DOCUMENT_ROOT'] . '/ext_routing.conf.inc';
        $this->_ext_conf = include_once
            $_SERVER['DOCUMENT_ROOT'] . '/ext.conf.inc';
    }

    /**
     * Main method called by the server.
     */
    function main()
    {
        $srv = $_SERVER;

        // Complete information on the incoming request. May be used to
        // determine conditions under which extensions are applied.
        $req = $this->parseRequest();
        //echo "\nRequest:"; print_r($req);

        foreach($this->_ext_route_conf as $route) {
            // Apply the first route that satisfies all conditions.
            if ($this->parseRouteConditions($route)) {
                break;
            }
        }
        $queue = $this->enqueueExtensions($route);
        //echo "Queue:"; print_r($queue);

        $data = [
            'post_data' => $req['post_data'],
            'uri' => $req['uri'],
            'files' => $req['files'],
        ];
        //echo "Data:"; print_r($data);

        return $this->processQueue($queue, $data);
    }


    /**
     * Parse relevant request elements and build an array.
     */
    function parseRequest()
    {
        return [
            'uri' => $_SERVER['REQUEST_URI'],
            'parsed_uri' => parse_url($_SERVER['REQUEST_URI']),
            'method' => $_SERVER['REQUEST_METHOD'],
            'headers' => getallheaders(),
            'post_data' => $_POST ?: null,
            'files' => $_FILES ?: null,
            // More parsers, e.g. file size, checksum, magic number or
            // payload parsers for JSON, RDF, XML...
            // Some resource-intensive parsers should be optional, and a config
            // file would be needed to activate or deactivate the desired ones.
        ];
    }


    /**
     * Build a queue based on extension conditions.
     */
    function enqueueExtensions($route)
    {
        $queue = [];
        foreach ($route['extensions'] as $ext_name => $ext_conf) {
            // Verify that the extension is activated in config.

            // Grab extension configuration.
            $ext = $this->_ext_conf[$ext_name];

            // If the result of parsing conditions is false, skip this
            // extension.
            if (
                !$ext['active']
                || !$this->parseExtConditions($ext_conf['conditions']))
            {
                continue;
            }

            // Add the extension to the queue.
            $queue []= $ext;
        }

        //echo "Queue:"; print_r($queue);
        return $queue;
    }


    /**
     * Parse the conditions for a route.
     */
    function parseRouteConditions($conditions)
    {
        return true;
    }


    /**
     * Parse the conditions for individual extensions in the router config.
     */
    function parseExtConditions($conditions)
    {
        return true;
    }


    function processQueue($queue, $data) {
        foreach ($queue as $ext) {
            // Invoke service
            //echo "\nExtension config:"; print_r($ext);
            $svc = $ext['service'];
            $svc_res = $this->callService($svc['uri'], $svc['method'], $data);
            // If the service returns a body, populate the post data for the
            // next request with it, otherwise use the same data.
            //echo "\nResponse from service: "; print_r($svc_res);
            if (
                array_key_exists('response', $svc_res)
                && !empty($svc_res['response'])
            ) {
                $data['post_data'] = $svc_res['response'];
            }

            // This is the "Validation pass?" step in the flow diagram. It is
            // generealized here to include a wide range of possible outcomes.
            $ret = $this->routeSvcResponse($svc_res['ret_code'],
                    $svc['response_router']);
            //echo "\ndata after route service response:"; print_r($data);

            // After the actions from routeSvcResponse are executed (if any)
            // the output of this method is used as the input for the next
            // extension. If this is the last extension processed, the output
            // is forwarded to the client.
        }
        http_response_code($svc_res['ret_code']);
        echo $data['post_data'];
        exit;
    }


    /**
     * Call the external service.
     */
    function callService($url, $method, $data)
    {
        $ch = curl_init($url);
        curl_setopt_array(
            $ch, [
                CURLOPT_RETURNTRANSFER => true,
            ]
        );

        if ($method == 'POST') {
            //echo "\nData: "; print_r($data);
            curl_setopt_array(
                $ch, [
                CURLOPT_POST => true,
                CURLOPT_POSTFIELDS => $data['post_data'],
                ]
            );
        }

        $res = curl_exec($ch);

        // Collect response (in case of a transformation service) and HTTP
        // response code.
        $ret = [
            'response' => $res,
            'ret_code' => curl_getinfo($ch, CURLINFO_HTTP_CODE),
        ];
        curl_close($ch);

        return $ret;
    }


    /**
     * Route the response from the service to the destination specified in
     * the configuration.
     */
    function routeSvcResponse($code, $routes)
    {
        //echo "\nCore:routeSvcResponse: Received response from service: ";
        //print_r($code);
        if (array_key_exists($code, $routes)) {
            $route_code = $routes[$code];
        } else if (array_key_exists('*', $routes)) {
            // '*' is a wildcard.
            $route_code = $routes['*'];
        }
        switch ($route_code) {
        case 'forward':
            // Continue queuing.
            return true;
            break;
        case 'send_error':
            $this->sendError();
            break;
        }
    }


    /**
     * Send an error code to the client.
     */
    function sendError(){
        http_response_code(400);
        echo "Your request was not groovy. Sorry.";
        exit;
    }
}

