package org.usfirst.frc.team1745.robot;

import edu.wpi.first.wpilibj.Compressor;

public class Pneumatics {

	Compressor compressor;

	public Pneumatics(int PCMID) {
		compressor = new Compressor(PCMID);
		compressor.setClosedLoopControl(true);
	}

}
