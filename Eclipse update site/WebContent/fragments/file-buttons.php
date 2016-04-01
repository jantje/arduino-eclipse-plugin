<?php
if (isset ( $_GET ["ver"] ) && isset ( $_GET ["arch"] ))
	listFiles ( "V" . $_GET ["ver"] . '_' . $_GET ["arch"] );
function my_offset($curfile, $prefix) {
	preg_match ( '/\d/', $curfile, $m, PREG_OFFSET_CAPTURE, strlen ( $prefix ) );
	if (sizeof ( $m ))
		return $m [0] [1];
	return strlen ( $prefix );
}
function my_bitness($curfile, $prefix) {
	return substr ( $curfile, my_offset ( $curfile, $prefix ), 2 );
}
function listFiles($prefix) {
	global $filter;
	$filter = $prefix;
	date_default_timezone_set ( 'UTC' );
	$location = "/customers/0/9/d/baeyens.it/httpd.www/eclipse/download/product";
	$dir = opendir ( $location );
	while ( false != ($file = readdir ( $dir )) ) {
		if (($file != ".") and ($file != "..") and ($file != "index.php")) {
			$files [] = $location . "/" . $file; // put in array.
		}
	}
	closedir ( $dir );
	$files = array_filter ( $files, "filter" );
	sort ( $files );
	foreach ( $files as &$file ) {
		$curfile = basename ( $file );
		echo '<div class="text-center col-md-4 col-md-offset-4">';
		echo '  <a href="http://eclipse.baeyens.it/download/product/' . $curfile . '" class="btn btn-success btn-lg text-center">Download <b>' . my_bitness ( $curfile, $prefix ) . ' bits</b> Bundle <i class="glyphicon glyphicon-cloud-download"></i></a>';
		echo '</div>';
	}
}
function filter($file) {
	global $filter;
	return (substr ( basename ( $file ), 0, strlen ( $filter ) ) == $filter);
}
?>