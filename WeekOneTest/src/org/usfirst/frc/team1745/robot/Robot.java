package org.usfirst.frc.team1745.robot;

import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.usfirst.frc.team1745.robot.P51Talon.Breakers;
import org.usfirst.frc.team1745.robot.P51Talon.Motors;

import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.wpilibj.AnalogInput;
import edu.wpi.first.wpilibj.CameraServer;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.RobotDrive.MotorType;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.vision.VisionThread;

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
	P51Talon LFDrive, RFDrive, LMDrive, RMDrive, LBDrive, RBDrive, GearIntake, LeftSpinner, RightSpinner, lifter;
	RobotDrive driveTrain;
	BallShifter rightDrive, leftDrive;
	Joystick joy1, joy2, joy3;
	// Pneumatics pneumatics;
	DoubleSolenoid shifter, grabber, arm;
	PowerDistributionPanel pdp;
	boolean highGear;
	DigitalInput frontBeam, backBeam;
	double grabbedTime;
	boolean isInWaitingMode = false;
	boolean intake = false, holding = true, placing = false;
	AnalogInput ultrasonicSensor;

	private static final int IMG_WIDTH = 640;
	private static final int IMG_HEIGHT = 480;
	private static final double SONIC_RATE = 5 / 512;
	NetworkTable table;
	double[] heights, centers;

	/**
	 * This function is run when the robot is first started up and should be
	 * used for any initialization code.
	 */

	public Robot() {
		table = NetworkTable.getTable("GRIP/myContoursReport");
	}

	@Override
	public void robotInit() {
		chooser.addDefault("Default Auto", defaultAuto);
		chooser.addObject("My Auto", customAuto);
		SmartDashboard.putData("Auto choices", chooser);
		LFDrive = new P51Talon(3, "Forward Left Motor", Motors.CIM, Breakers.amp40, 3); //
		RFDrive = new P51Talon(0, "Forward Right Motor", Motors.CIM, Breakers.amp40, 2); //
		LMDrive = new P51Talon(4, "Middle Left Motor", Motors.CIM, Breakers.amp40, 13); //
		RMDrive = new P51Talon(1, "Middle Right Motor", Motors.CIM, Breakers.amp40, 14); //
		LBDrive = new P51Talon(5, "Back Left Motor", Motors.CIM, Breakers.amp40, 1); //
		RBDrive = new P51Talon(2, "Back Right Motor", Motors.CIM, Breakers.amp40, 12); //
		lifter = new P51Talon(6, "Gear Intake Roller", Motors.CIM, Breakers.amp40, 0);
		GearIntake = new P51Talon(7, "Gear Intake Roller", Motors.bag, Breakers.amp40, 15);
		LeftSpinner = new P51Talon(8, "Left Spinner", Motors.bag, Breakers.amp20, 4);
		RightSpinner = new P51Talon(9, "Right Spinner", Motors.bag, Breakers.amp20, 11);
		rightDrive = new BallShifter(RFDrive, RMDrive, RBDrive);
		leftDrive = new BallShifter(LFDrive, LMDrive, LBDrive);
		driveTrain = new RobotDrive(LFDrive, RFDrive);
		driveTrain.setInvertedMotor(MotorType.kRearLeft, true);
		driveTrain.setInvertedMotor(MotorType.kRearRight, true);
		joy1 = new Joystick(0);
		joy2 = new Joystick(1);
		joy3 = new Joystick(2);
		// pneumatics = new Pneumatics();
		shifter = new DoubleSolenoid(0, 1);
		grabber = new DoubleSolenoid(2, 3);
		arm = new DoubleSolenoid(4, 5);
		highGear = false;
		frontBeam = new DigitalInput(0);
		backBeam = new DigitalInput(1);
		ultrasonicSensor = new AnalogInput(2);
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

		intake = false;
		holding = true;
		placing = false;
		SmartDashboard.putString("Mode", "holding");
	}

	/**
	 * This function is called periodically during operator control
	 */
	@Override
	public void teleopPeriodic() {
		SmartDashboard.putNumber("Ultrasound Distance", ultrasonicSensor.getVoltage() / SONIC_RATE);
		manageStates();
		actOnState();
		pullTables();
		double centerX = 320;
		double distance = 0;
		if (centers.length >= 2 && heights.length >= 2) {
			centerX = (centers[0] + centers[1]) / 2;
			distance = 3500 / ((heights[0] + heights[1]) / 2);
			System.out.println("Distance: " + distance);
		}
		if (centers.length == 1 && heights.length == 1) {
			centerX = centers[0];
			distance = 3500 / heights[0];
			System.out.println("Distance: " + distance);
		}

		double turn = (centerX - IMG_WIDTH / 2);

		System.out.println("Turn: " + turn + " CenterX: " + centerX);
		if (joy1.getTrigger()) {
			driveTrain.arcadeDrive(-.75, turn * .005);
		} else {
			driveTrain.tankDrive(joy1.getY(), joy2.getY());
		}

		// Gear Control
		if (joy2.getTrigger()) {
			shifter.set(DoubleSolenoid.Value.kForward);
		} else {
			shifter.set(DoubleSolenoid.Value.kReverse);
		}

		if (joy1.getRawButton(3)) {
			lifter.set(1);
		} else {
			lifter.set(0);
		}

		talonsToDashboard();

	}

	private void manageStates() {
		// TODO Auto-generated method stub
		if (joy3.getRawButton(1)) {
			intake = true;
			holding = false;
			placing = false;
			SmartDashboard.putString("Mode", "intake");
		}
		if (joy3.getRawButton(2)) {
			intake = false;
			holding = true;
			placing = false;
			SmartDashboard.putString("Mode", "holding");
		}
		if (joy3.getRawButton(3)) {
			intake = false;
			holding = false;
			placing = true;
			SmartDashboard.putString("Mode", "placing");
		}
	}
	
	public void actOnState(){
		if (intake == true) {
			System.out.println("I should be in intake");
			GearIntake.set(1);
			//TODO arm and grabber are one another...change names in code
			arm.set(DoubleSolenoid.Value.kForward);
			grabber.set(DoubleSolenoid.Value.kReverse);
			LeftSpinner.set(-0.5);
			RightSpinner.set(0.5);
			
			if (!frontBeam.get()) {

				System.out.println("I should be in intake - break beam broken");
				if (!isInWaitingMode) {
					grabbedTime = System.currentTimeMillis();
					isInWaitingMode = true;
				}
				arm.set(DoubleSolenoid.Value.kReverse);
				if (System.currentTimeMillis() >= grabbedTime + 1000) {
					GearIntake.set(0);
				}
			}
		}
		if (holding == true) {

			System.out.println("I should be in holding");
			GearIntake.set(0);
			LeftSpinner.set(0);
			RightSpinner.set(0);
			grabber.set(DoubleSolenoid.Value.kReverse);
			arm.set(DoubleSolenoid.Value.kForward);
		}
		if (placing == true) {
		System.out.println("I should be in place");
			GearIntake.set(0);
			LeftSpinner.set(0);
			RightSpinner.set(0);
			grabber.set(DoubleSolenoid.Value.kForward);
			arm.set(DoubleSolenoid.Value.kReverse);
		}
	}

	public void pullTables(){
		heights = table.getNumberArray("height", new double[] { -1 });
		centers = table.getNumberArray("centerX", new double[] { -1 });
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

	public void nathanAuto() {
		pullTables();
		final double HORIZONTAL_FIELD_OF_VIEW = 59.7; // degrees (also found
		// 59.7, we should
		// calibrate)
		final double VERTICAL_FIELD_OF_VIEW = 41.4; // degrees TODO calibrate
		final double DEGREES_TO_RADIANS = 0.01745329252;
		final double RADIANS_TO_DEGREES = 57.295779513;
		final double TARGET_HEIGHT_INCHES = 5.0; // inches
		final double TARGET_WIDTH_INCHES = 2.0; // inches
		final double TARGET_W_T_H_NORMAL = TARGET_WIDTH_INCHES / TARGET_HEIGHT_INCHES;
		final double MINIMUM_TURN_ANGLE = 5.0; // degrees

		// calculations
		double averageCenter = 0.0;
		double averageHeight = 0.0;

		boolean isDistanceValid = false;

		// Get average center and height
		if (centers.length >= 2 && heights.length >= 2) {
			averageCenter = centers[0] + centers[1] / 2.0;
			averageHeight = heights[0] + heights[1] / 2.0;
			isDistanceValid = true;
		} else if (centers.length == 1 && heights.length == 1) {
			averageCenter = centers[0];
			averageHeight = heights[0];
			isDistanceValid = true;
		} else {
			// do nothing
		}

		double absoluteAngle = averageCenter / IMG_WIDTH * HORIZONTAL_FIELD_OF_VIEW;
		double relativeAngle = absoluteAngle - HORIZONTAL_FIELD_OF_VIEW / 2.0;

		if (isDistanceValid) {
			System.out.println("Relative Angle from robot to target (degrees): " + relativeAngle);
		}

		double distanceCalculationFRC = TARGET_HEIGHT_INCHES * IMG_HEIGHT
				/ (2 * averageHeight * Math.tan((VERTICAL_FIELD_OF_VIEW / 2) * DEGREES_TO_RADIANS));

		if (isDistanceValid) {
			System.out.println("Distance from robot to target (inches): " + distanceCalculationFRC);
		}

		// experimental code to find angle of target with respect to robot
		double[] widths;
		widths = table.getNumberArray("width", new double[] { -1 });
		double averageWtH = -1.0;

		boolean isWidthToHeightValid = false;

		if (widths.length >= 2 && heights.length >= 2) {
			averageWtH = ((widths[0] / heights[0]) + (widths[1] / heights[1])) / 2.0;
			isWidthToHeightValid = true;
		} else if (widths.length == 1 && heights.length == 1) {
			averageWtH = (widths[0] / heights[0]);
			isWidthToHeightValid = true;
		} else {
			// do nothing
		}
		if (isWidthToHeightValid) {
			System.out.println("Average width to height ratio: " + averageWtH);
		}

		// rough calculation
		double roughAngleMeasurement = averageWtH / TARGET_W_T_H_NORMAL;

		double amountToTurn = (1 - roughAngleMeasurement) * HORIZONTAL_FIELD_OF_VIEW;

		if (amountToTurn < MINIMUM_TURN_ANGLE) {
			amountToTurn = 0.0;
		}

		if (isWidthToHeightValid) {
			System.out.println("!!NathanDrive!! turn: " + amountToTurn);
		}

	}

}
