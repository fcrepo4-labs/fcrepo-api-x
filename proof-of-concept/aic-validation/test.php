<?php
loop();

function loop(){
	for($i=0; $i<10; $i++) {
		skipOdd($i);
	}
}

function skipOdd($i) {
	if($i % 2 == 0) {
		echo "$i\n";
	} else {
		continue 1;
	}
}
