/**
* Name: Water flowing in the red river bed
* Author: drogoul
* Tags: 
*/


model Terrain

global parent: physical_world {

	// We scale the DEM up a little
	float z_scale <- 0.5;
	float step <-  1.0/30;	
	bool flowing <- true;
	point gravity <- {-z_scale/4, z_scale, -9.81};
	int number_of_water_units <- 1 min: 0 max: 10;
	list<point> origins_of_flow <- [{17,3}, {55,3}];
	matrix<float> terrain <- matrix<float>(grid_file("../images/DEM/RedRiver.asc"));

	geometry shape <- box({terrain.columns, terrain.rows, max(terrain)*z_scale});
	float friction <- 0.0;
	float restitution <- 0.5;


	init {
		do register([self]);
	 	write "" + shape.width + "x" + shape.height;
	}

	reflex flow {
			loop origin_of_flow over: origins_of_flow {
				float x <- min(terrain.columns - 1, max(0, origin_of_flow.x + rnd(10) - 5));
				float y <- min(terrain.rows - 1, max(0, origin_of_flow.y + rnd(10) - 5));
				point p <- origin_of_flow + {rnd(10) - 5, rnd(10 - 5), terrain[{x, y}] + 4};
				create water number: number_of_water_units with: [location::p];
			}
	}
}

species water skills: [dynamic_body] {
	geometry shape <- sphere(1.0);
	float friction <- 0.0;
	float damping <- 0.0;
	float mass <- 0.5;
	rgb color <- one_of(brewer_colors("Blues"));
	

	aspect default {
		if (location.y > 10){
		draw shape color: color;}
	}
	
		
	reflex manage_location when: location.z < -20 {
		do die;
	}

} 

experiment "3D view" type: gui {
	
	action _init_ {
		create simulation with: [z_scale::0.3];
		create simulation with: [z_scale::1.0];
		create simulation with: [z_scale::2.0];
		create simulation with: [z_scale::3.0];
	} 
	
	parameter "Number of water agents per cycle" var: number_of_water_units;
	
	output {
		layout #split;
		display "Flow" type: opengl background: #white camera_location: {90.0,184.4875,144.4624} camera_target: {90.0,67.5,0.0} camera_orientation: {0.0,0.7771,0.6293} antialias: false {
			graphics world {
				draw "Scale: " + z_scale color: #cadetblue font: font("Helvetica", 18, #bold) at: {world.location.x, -10, 25} anchor: #center depth: 2 rotate: -90::{1,0,0};
				draw aabb wireframe: true color: #lightblue;
			}
			mesh terrain grayscale: true triangulation: true refresh: false scale: z_scale;
			species water;
			event #mouse_down {
				point p <- #user_location;
				origins_of_flow << {p.x, p.y};
			}
		}

	}}
	





experiment name type: gui {

	
	// Define parameters here if necessary
	// parameter "My parameter" category: "My parameters" var: one_global_attribute;
	
	// Define attributes, actions, a init section and behaviors if necessary
	// init { }
	
	
	output {
	// Define inspectors, browsers and displays here
	
	// inspect one_or_several_agents;
	//
	// display "My display" { 
	//		species one_species;
	//		species another_species;
	// 		grid a_grid;
	// 		...
	// }

	}
}