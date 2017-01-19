package org.usfirst.frc.team1745.robot;

import com.ctre.CANTalon;
import com.ctre.CANTalon.TalonControlMode;

public class BallShifter {

	CANTalon front, middle, rear;

	public BallShifter(CANTalon front, CANTalon middle, CANTalon rear) {
		this.front = front;
		this.middle = middle;
		this.rear = rear;
		this.front.changeControlMode(TalonControlMode.PercentVbus);
		this.middle.changeControlMode(TalonControlMode.Follower);
		this.middle.set(this.front.getDeviceID());
		this.rear.changeControlMode(TalonControlMode.Follower);
		this.rear.set(this.front.getDeviceID());
	}

}
