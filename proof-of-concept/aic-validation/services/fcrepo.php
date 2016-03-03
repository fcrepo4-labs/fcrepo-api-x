<?php
/**
 * The most successful Fedora implementation on the block.
 * It writes anything you throw at it to disk and sends a 201 on success
 * and a 500 on failure.
 */

$store_file = getenv('TMPDIR') . '/apix_poc.fcrepo.out';

//echo "POST: "; print_r($_POST);
$dump = "\n" . date(DATE_ATOM) . " - " . serialize($_POST);
if (file_put_contents($store_file, $dump, FILE_APPEND)) {
    http_response_code(201);
    echo "Hunky dory.";
} else {
    http_response_code(500);
    echo "Something got bad vibes here.";
}

?>
