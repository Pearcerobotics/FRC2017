package org.usfirst.frc.team1745.robot;

import edu.wpi.first.wpilibj.Compressor;

public class Pneumatics {

	Compressor compressor;

	public Pneumatics() {
		compressor = new Compressor(0);
		compressor.setClosedLoopControl(true);

	}

}
