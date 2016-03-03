<?php
$srv = $_SERVER;

$rpath = $srv['REQUEST_URI'];
$path = $srv['DOCUMENT_ROOT'];
//echo $rpath;

switch($rpath){
case '/core':
    include_once $path . '/core.php';
    $m = new Core;
    break;
case '/validation':
    include_once $path . '/services/validation.php';
    $m = new Validation;
    break;
case '/fcrepo':
    include_once $path . '/services/fcrepo.php';
    $m = new FCRepo;
    break;
default:
    echo "$rpath is not a valid URI.";
}
//echo "Executing class: "; print_r($m);
$m->main();
?>
