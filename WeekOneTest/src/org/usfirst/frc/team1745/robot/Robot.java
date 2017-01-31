package org.usfirst.frc.team1745.robot;

import org.usfirst.frc.team1745.robot.P51Talon.Breakers;
import org.usfirst.frc.team1745.robot.P51Talon.Motors;

import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot {
	final String defaultAuto = "Default";
	final String customAuto = "My Auto";
	String autoSelected;
	SendableChooser<String> chooser = new SendableChooser<String>();
	P51Talon LFDrive, RFDrive, LMDrive, RMDrive, LBDrive, RBDrive;
	RobotDrive driveTrain;
	BallShifter rightDrive, leftDrive;
	Joystick joy1, joy2;
	// Pneumatics pneumatics;
	DoubleSolenoid shifter;
	PowerDistributionPanel pdp;
	boolean highGear;

	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */
	@Override
	public void robotInit() {
		chooser.addDefault("Default Auto", defaultAuto);
		chooser.addObject("My Auto", customAuto);
		SmartDashboard.putData("Auto choices", chooser);
		LFDrive = new P51Talon(0, "Forward Left Motor", Motors.CIM, Breakers.amp40, 15);
		RFDrive = new P51Talon(1, "Forward Right Motor", Motors.CIM, Breakers.amp40, 14);
		LMDrive = new P51Talon(4, "Middle Left Motor", Motors.CIM, Breakers.amp40, 1);
		RMDrive = new P51Talon(5, "Middle Right Motor", Motors.CIM, Breakers.amp40, 0);
		LBDrive = new P51Talon(2, "Back Left Motor", Motors.CIM, Breakers.amp40, 13);
		RBDrive = new P51Talon(3, "Back Right Motor", Motors.CIM, Breakers.amp40, 12);
		rightDrive = new BallShifter(RFDrive, RMDrive, RBDrive);
		leftDrive = new BallShifter(LFDrive, LMDrive, LBDrive);
		driveTrain = new RobotDrive(LFDrive, RFDrive);
		joy1 = new Joystick(0);
		joy2 = new Joystick(1);
		// pneumatics = new Pneumatics();
		shifter = new DoubleSolenoid(0, 1);
		highGear = false;
	}

	/**
	 * This autonomous (along with the chooser code above) shows how to select
	 * between different autonomous modes using the dashboard. The sendable
	 * chooser code works with the Java SmartDashboard. If you prefer the
	 * LabVIEW Dashboard, remove all of the chooser code and uncomment the
	 * getString line to get the auto name from the text box below the Gyro
	 *
	 * You can add additional auto modes by adding additional comparisons to the
	 * switch structure below with additional strings. If using the
	 * SendableChooser make sure to add them to the chooser code above as well.
	 */
	@Override
	public void autonomousInit() {
		autoSelected = chooser.getSelected();
		autoSelected = SmartDashboard.getString("Auto Selector", defaultAuto);
		System.out.println("Auto selected: " + autoSelected);
	}

	/**
	 * This function is called periodically during autonomous
	 */
	@Override
	public void autonomousPeriodic() {
		switch (autoSelected) {
		case customAuto:
			// Put custom auto code here
			break;
		case defaultAuto:
		default:
			// Put default auto code here
			break;
		}
	}

	public void teleopInit() {
	}

	/**
	 * This function is called periodically during operator control
	 */
	@Override
	public void teleopPeriodic() {

		driveTrain.tankDrive(joy1, joy2, true);
		if (joy1.getTrigger() == true) {
			shifter.set(DoubleSolenoid.Value.kForward);
			highGear = true;
		} else if (joy1.getTrigger() == false) {
			shifter.set(DoubleSolenoid.Value.kReverse);
			highGear = false;
		}

		talonsToDashboard();
		SmartDashboard.putBoolean("High Gear", highGear);
		// SmartDashboard.putBoolean("Pressure Switch",
		// pneumatics.compressor.getPressureSwitchValue());
		// SmartDashboard.putBoolean("Compressor",
		// pneumatics.compressor.enabled());

	}

	/**
	 * This function is called periodically during test mode
	 */
	@Override
	public void testPeriodic() {
	}

	public void talonsToDashboard() {
		LFDrive.toDashboard();
		RFDrive.toDashboard();
	}

}
