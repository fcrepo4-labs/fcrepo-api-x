<?php
/**
 * Mock validation service.
 *
 * All services must send back:
 * 1. An HTTP return code indicating the outcome of the service. This codes is
 * then interpreted by the Core based on the extension configuration to further
 * route the request.
 * 2. A body consisting of a raw string with the input data transformed, OR
 * an empty result (such as in the case of this example, where data is not
 * manuipulated).
 */

// Execute main method.
//echo "Validation service";
$m = new ValidationService;
$m->main();

/**
 * Validation class.
 *
 * It only implements two validation methods: data type and cardinality.
 */
class ValidationService
{
    function main()
    {
        if ($this->validate()) {
            echo "\nValidation: Validation pass.";
            http_response_code(204);
        } else {
            echo "\nValidation: Validation failed.";
            http_response_code(412);
        }
        // Data does not need to be passed back since it is unchanged in any case.
    }

    function validate()
    {
        $conf = include './services/validation.conf.inc';

        // Iterate over input data.
        $data = json_decode($_POST['properties'], true);
        echo "\nInput data: "; print_r($data);
        foreach ($data as $prop_name => $prop_values) {
            // Verify if the property name is in the validation config.
            if (array_key_exists($prop_name, $conf)) {
                // Iterate over validation rules and apply each one of them.
                foreach ($conf[$prop_name] as $rule_n => $rule_v) {
                    echo "\nValidation: Validating property $prop_name for rule: $rule_n\n";
                    echo "\nValidation: rule value: "; print_r($rule_v);
                    try {
                        switch($rule_n) {
                        case 'datatype':
                            $this->validateDatatype($prop_values, $rule_v);
                            break;
                        case 'cardinality':
                            $this->validateCardinality($prop_values, $rule_v);
                            break;
                        }
                    } catch(Exception $e) {
                        // If validation raises an exception, interrupt the
                        // process and return false.
                        return false;
                    }
                }
            }
        }
        // If all goes smooth, return true.
        return true;
    }


    function validateCardinality($values, $limit)
    {
        echo "\nvalidateCardinality: values:"; print_r($values);
        echo "\nvalidateCardinality: limit:"; print_r($limit);
        $l = sizeof($values);
        if ((array_key_exists('min', $limit) && $l < $limit['min'])
            || (array_key_exists('max', $limit) && $l > $limit['max'])
        ) {
            throw new Exception();
        }
    }

    function validateDatatype($values, $types)
    {
        //echo "\nValidation: Values:"; print_r($values);
        foreach ($values as $v) {
            $type = gettype($v);
            //echo "\nValidation: data type for property $v: $type";
            if (!in_array($type, $types)) {
                throw new Exception();
            }
        }
    }
}
?>
