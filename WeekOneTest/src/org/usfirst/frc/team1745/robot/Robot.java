package org.usfirst.frc.team1745.robot;

import org.usfirst.frc.team1745.robot.P51Talon.Breakers;
import org.usfirst.frc.team1745.robot.P51Talon.Motors;

import edu.wpi.first.wpilibj.AnalogInput;
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

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends IterativeRobot
{
    enum GearGrabberMode
    {
        INTAKE, HOLDING, PLACING, EJECTING
    }

    final String defaultAuto = "Default";
    final String customAuto = "My Auto";
    String autoSelected;
    SendableChooser<String> chooser = new SendableChooser<String>();
    P51Talon LFDrive, RFDrive, LMDrive, RMDrive, LBDrive, RBDrive, gearIntakeMotor, leftConveyorSpinner, rightConveyerSpinner,
            lifter;
    RobotDrive driveTrain;
    BallShifter rightDrive, leftDrive;
    Joystick joy1, joy2, joy3;
    DoubleSolenoid shifter; // Reverse is low gear, forward is high gear
    DoubleSolenoid armRotatingPistons; // Reverse is the flat on the ground
                                       // position, forward is 90 degree upright
    DoubleSolenoid gearCompressingPistons; // Reverse is clamping the gear,
                                           // forward is open to receive a gear
    PowerDistributionPanel pdp;
    boolean highGear;
    DigitalInput frontBeam, backBeam;
    double grabbedTime;
    boolean isInWaitingMode = false;

    AnalogInput ultrasonicSensor;

    private static final int IMG_WIDTH = 640;
    private static final int IMG_HEIGHT = 480;
    private static final double SONIC_RATE = 5 / 512;
    NetworkTable table;
    double[] heights, centers;
    GearGrabberMode gearGrabberMode;

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */

    public Robot()
    {
        this.table = NetworkTable.getTable("GRIP/myContoursReport");
    }

    @Override
    public void robotInit()
    {
        this.chooser.addDefault("Default Auto", this.defaultAuto);
        this.chooser.addObject("My Auto", this.customAuto);
        SmartDashboard.putData("Auto choices", this.chooser);
        this.LFDrive = new P51Talon(3, "Forward Left Motor", Motors.CIM, Breakers.amp40, 3); //
        this.RFDrive = new P51Talon(0, "Forward Right Motor", Motors.CIM, Breakers.amp40, 2); //
        this.LMDrive = new P51Talon(4, "Middle Left Motor", Motors.CIM, Breakers.amp40, 13); //
        this.RMDrive = new P51Talon(1, "Middle Right Motor", Motors.CIM, Breakers.amp40, 14); //
        this.LBDrive = new P51Talon(5, "Back Left Motor", Motors.CIM, Breakers.amp40, 1); //
        this.RBDrive = new P51Talon(2, "Back Right Motor", Motors.CIM, Breakers.amp40, 12); //
        this.lifter = new P51Talon(6, "Gear Intake Roller", Motors.CIM, Breakers.amp40, 0);
        this.gearIntakeMotor = new P51Talon(7, "Gear Intake Roller", Motors.bag, Breakers.amp40, 15);
        this.leftConveyorSpinner = new P51Talon(8, "Left Spinner", Motors.bag, Breakers.amp20, 4);
        this.rightConveyerSpinner = new P51Talon(9, "Right Spinner", Motors.bag, Breakers.amp20, 11);
        this.rightDrive = new BallShifter(this.RFDrive, this.RMDrive, this.RBDrive);
        this.leftDrive = new BallShifter(this.LFDrive, this.LMDrive, this.LBDrive);
        this.driveTrain = new RobotDrive(this.LFDrive, this.RFDrive);
        this.driveTrain.setInvertedMotor(MotorType.kRearLeft, true);
        this.driveTrain.setInvertedMotor(MotorType.kRearRight, true);
        this.joy1 = new Joystick(0);
        this.joy2 = new Joystick(1);
        this.joy3 = new Joystick(2);
        this.shifter = new DoubleSolenoid(0, 1);
        this.armRotatingPistons = new DoubleSolenoid(2, 3);
        this.gearCompressingPistons = new DoubleSolenoid(4, 5);
        this.highGear = false;
        this.frontBeam = new DigitalInput(0);
        this.backBeam = new DigitalInput(1);
        this.ultrasonicSensor = new AnalogInput(2);
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
    public void autonomousInit()
    {
        this.autoSelected = this.chooser.getSelected();
        this.autoSelected = SmartDashboard.getString("Auto Selector", this.defaultAuto);
        System.out.println("Auto selected: " + this.autoSelected);
    }

    /**
     * This function is called periodically during autonomous
     */
    @Override
    public void autonomousPeriodic()
    {
        switch (this.autoSelected)
        {
            case customAuto:
                // Put custom auto code here
                break;
            case defaultAuto:
            default:
                // Put default auto code here
                break;
        }
    }

    @Override
    public void teleopInit()
    {

        this.gearGrabberMode = GearGrabberMode.HOLDING;
        SmartDashboard.putString("Mode", "holding");
    }

    /**
     * This function is called periodically during operator control
     */
    @Override
    public void teleopPeriodic()
    {
        SmartDashboard.putNumber("Ultrasound Distance", this.ultrasonicSensor.getVoltage() / SONIC_RATE);
        manageStates();
        actOnState();
        pullTables();
        double centerX = 320;
        double distance = 0;
        if (this.centers.length >= 2 && this.heights.length >= 2)
        {
            centerX = (this.centers[0] + this.centers[1]) / 2;
            distance = 3500 / ((this.heights[0] + this.heights[1]) / 2);
            System.out.println("Distance: " + distance);
        }
        if (this.centers.length == 1 && this.heights.length == 1)
        {
            centerX = this.centers[0];
            distance = 3500 / this.heights[0];
            System.out.println("Distance: " + distance);
        }

        final double turn = (centerX - IMG_WIDTH / 2);

        System.out.println("Turn: " + turn + " CenterX: " + centerX);
        if (this.joy1.getTrigger())
        {
            this.driveTrain.arcadeDrive(-.75, turn * .005);
        }
        else
        {
            this.driveTrain.tankDrive(this.joy1.getY(), this.joy2.getY());
        }

        // Gear Control
        if (this.joy2.getTrigger())
        {
            this.shifter.set(DoubleSolenoid.Value.kForward);
        }
        else
        {
            this.shifter.set(DoubleSolenoid.Value.kReverse);
        }

        if (this.joy1.getRawButton(3))
        {
            this.lifter.set(1);
        }
        else
        {
            this.lifter.set(0);
        }

        talonsToDashboard();

    }

    private void manageStates()
    {
        if (this.joy3.getRawButton(1))
        {

            this.gearGrabberMode = GearGrabberMode.INTAKE;
            SmartDashboard.putString("Mode", "intake");
        }
        else if (this.joy3.getRawButton(2))
        {
            this.gearGrabberMode = GearGrabberMode.HOLDING;
            SmartDashboard.putString("Mode", "holding");
        }
        else if (this.joy3.getRawButton(3))
        {
            this.gearGrabberMode = GearGrabberMode.PLACING;
            SmartDashboard.putString("Mode", "placing");
        }
        else if (this.joy3.getRawButton(4))
        {
            this.gearGrabberMode = GearGrabberMode.EJECTING;
            SmartDashboard.putString("Mode", "ejecting");
        }
    }

    public void actOnState()
    {
        switch (this.gearGrabberMode)
        {
            case INTAKE:
            {
                this.gearIntakeMotor.set(1);
                this.gearCompressingPistons.set(DoubleSolenoid.Value.kForward);
                this.armRotatingPistons.set(DoubleSolenoid.Value.kReverse);
                this.leftConveyorSpinner.set(-0.5);
                this.rightConveyerSpinner.set(0.5);

                if (!this.frontBeam.get())
                {
                    if (!this.isInWaitingMode)
                    {
                        this.grabbedTime = System.currentTimeMillis();
                        this.isInWaitingMode = true;
                    }
                    this.gearCompressingPistons.set(DoubleSolenoid.Value.kReverse);
                    if (System.currentTimeMillis() >= this.grabbedTime + 1000)
                    {
                        this.gearIntakeMotor.set(0);
                    }
                }
                break;
            }
            case HOLDING:
            {
                this.gearIntakeMotor.set(0);
                this.leftConveyorSpinner.set(0);
                this.rightConveyerSpinner.set(0);
                this.armRotatingPistons.set(DoubleSolenoid.Value.kReverse);
                this.gearCompressingPistons.set(DoubleSolenoid.Value.kForward);
                break;
            }
            case PLACING:
            {
                this.gearIntakeMotor.set(0);
                this.leftConveyorSpinner.set(0);
                this.rightConveyerSpinner.set(0);
                this.armRotatingPistons.set(DoubleSolenoid.Value.kForward);
                this.gearCompressingPistons.set(DoubleSolenoid.Value.kReverse);
                break;
            }
            case EJECTING:
            {
                // TODO if we put the piston sensors back on, we can know our
                // position and know if we should lower the arm rotating piston
                this.gearIntakeMotor.set(-1);
                this.armRotatingPistons.set(DoubleSolenoid.Value.kForward);
                this.gearCompressingPistons.set(DoubleSolenoid.Value.kReverse);
                this.leftConveyorSpinner.set(-0.5);
                this.rightConveyerSpinner.set(0.5);
                break;
            }
            default:
            {
                // throw an error
                System.err.println("somehow hit the default case, not good");
                break;
            }
        }
    }

    public void pullTables()
    {
        this.heights = this.table.getNumberArray("height", new double[] { -1 });
        this.centers = this.table.getNumberArray("centerX", new double[] { -1 });
    }

    /**
     * This function is called periodically during test mode
     */
    @Override
    public void testPeriodic()
    {

    }

    public void talonsToDashboard()
    {
        this.LFDrive.toDashboard();
        this.RFDrive.toDashboard();
    }

    /**
     *
     */
    public void nathanAuto()
    {
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
        if (this.centers.length >= 2 && this.heights.length >= 2)
        {
            averageCenter = this.centers[0] + this.centers[1] / 2.0;
            averageHeight = this.heights[0] + this.heights[1] / 2.0;
            isDistanceValid = true;
        }
        else if (this.centers.length == 1 && this.heights.length == 1)
        {
            averageCenter = this.centers[0];
            averageHeight = this.heights[0];
            isDistanceValid = true;
        }
        else
        {
            // do nothing
        }

        final double absoluteAngle = averageCenter / IMG_WIDTH * HORIZONTAL_FIELD_OF_VIEW;
        final double relativeAngle = absoluteAngle - HORIZONTAL_FIELD_OF_VIEW / 2.0;

        if (isDistanceValid)
        {
            System.out.println("Relative Angle from robot to target (degrees): " + relativeAngle);
        }

        final double distanceCalculationFRC = TARGET_HEIGHT_INCHES * IMG_HEIGHT
                / (2 * averageHeight * Math.tan((VERTICAL_FIELD_OF_VIEW / 2) * DEGREES_TO_RADIANS));

        if (isDistanceValid)
        {
            System.out.println("Distance from robot to target (inches): " + distanceCalculationFRC);
        }

        // experimental code to find angle of target with respect to robot
        double[] widths;
        widths = this.table.getNumberArray("width", new double[] { -1 });
        double averageWtH = -1.0;

        boolean isWidthToHeightValid = false;

        if (widths.length >= 2 && this.heights.length >= 2)
        {
            averageWtH = ((widths[0] / this.heights[0]) + (widths[1] / this.heights[1])) / 2.0;
            isWidthToHeightValid = true;
        }
        else if (widths.length == 1 && this.heights.length == 1)
        {
            averageWtH = (widths[0] / this.heights[0]);
            isWidthToHeightValid = true;
        }
        else
        {
            // do nothing
        }
        if (isWidthToHeightValid)
        {
            System.out.println("Average width to height ratio: " + averageWtH);
        }

        // rough calculation
        final double roughAngleMeasurement = averageWtH / TARGET_W_T_H_NORMAL;

        double amountToTurn = (1 - roughAngleMeasurement) * HORIZONTAL_FIELD_OF_VIEW;

        if (amountToTurn < MINIMUM_TURN_ANGLE)
        {
            amountToTurn = 0.0;
        }

        if (isWidthToHeightValid)
        {
            System.out.println("!!NathanDrive!! turn: " + amountToTurn);
        }

    }

}
