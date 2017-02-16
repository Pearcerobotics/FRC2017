package org.usfirst.frc.team1745.robot;

import com.ctre.CANTalon;
import com.ctre.CANTalon.TalonControlMode;

import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
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
    final String defaultAuto = "Default";
    final String customAuto = "My Auto";
    String autoSelected;
    SendableChooser<String> chooser = new SendableChooser<>();
    CANTalon LFDrive, RFDrive, LBDrive, RBDrive;
    RobotDrive driveTrain;
    Joystick joy1, joy2;

    private static final int IMG_WIDTH = 640;
    private static final int IMG_HEIGHT = 480;
    NetworkTable table;

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */

    public Robot()
    {
        table = NetworkTable.getTable("GRIP/myContoursReport");
    }

    public void robotInit()
    {
        chooser.addDefault("Default Auto", defaultAuto);
        chooser.addObject("My Auto", customAuto);
        SmartDashboard.putData("Auto choices", chooser);
        LFDrive = new CANTalon(0);
        RFDrive = new CANTalon(1);
        LBDrive = new CANTalon(2);
        RBDrive = new CANTalon(3);
        LBDrive.changeControlMode(TalonControlMode.Follower);
        LBDrive.set(LFDrive.getDeviceID());
        RBDrive.changeControlMode(TalonControlMode.Follower);
        RBDrive.set(RFDrive.getDeviceID());
        driveTrain = new RobotDrive(LFDrive, RFDrive);
        try
        {
            driveTrain.setInvertedMotor(MotorType.kRearLeft, true);
            driveTrain.setInvertedMotor(MotorType.kRearRight, true);
        }
        catch (Exception e)
        {
            System.out.println("Exception inverting motors");
        }
        joy1 = new Joystick(0);
        joy2 = new Joystick(1);

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
        autoSelected = chooser.getSelected();
        // autoSelected = SmartDashboard.getString("Auto Selector",
        // defaultAuto);
        System.out.println("Auto selected: " + autoSelected);
    }

    @Override
    public void teleopInit()
    {

    }

    /**
     * This function is called periodically during autonomous
     */
    @Override
    public void autonomousPeriodic()
    {
        driveTrain.arcadeDrive(.1, 0);
    }

    /**
     * This function is called periodically during operator control
     */
    @Override
    public void teleopPeriodic()
    {
        double[] heights, centers;
        heights = table.getNumberArray("height", new double[] { -1 });
        centers = table.getNumberArray("centerX", new double[] { -1 });
        double centerX = 320;
        double distance = 0;
        if (centers.length >= 2 && heights.length >= 2)
        {
            centerX = (centers[0] + centers[1]) / 2;
            distance = 3500 / ((heights[0] + heights[1]) / 2);
            System.out.println("Distance: " + distance);
        }
        if (centers.length == 1)
        {
            centerX = centers[0];
            distance = 3500 / heights[0];
            System.out.println("Distance: " + distance);
        }

        double turn = (centerX - IMG_WIDTH / 2);

        // Code from Nathan to determine distance and angle to the target
        // constants
        final double HORIZONTAL_FIELD_OF_VIEW = 50.6496; // degrees (also found
                                                         // 59.7, we should
                                                         // calibrate)
        final double VERTICAL_FIELD_OF_VIEW = 39.3072; // degrees TODO calibrate
        final double DEGREES_TO_RADIANS = 0.01745329252;
        final double RADIANS_TO_DEGREES = 57.295779513;
        final double TARGET_HEIGHT_INCHES = 7.0; // inches
        final double TARGET_WIDTH_INCHES = 2.0; // inches
        final double TARGET_W_T_H_NORMAL = TARGET_WIDTH_INCHES / TARGET_HEIGHT_INCHES;
        final double MINIMUM_TURN_ANGLE = 5.0; // degrees

        // calculations
        double averageCenter = 0.0;
        double averageHeight = 0.0;

        // Get average center and height
        if (centers.length >= 2)
        {
            averageCenter = centers[0] + centers[1] / 2.0;
            averageHeight = heights[0] + heights[1] / 2.0;
        }
        else if (centers.length == 1)
        {
            averageCenter = centers[0];
            averageHeight = heights[0];
        }
        else
        {
            // do nothing
        }

        double absoluteAngle = averageCenter / IMG_WIDTH * HORIZONTAL_FIELD_OF_VIEW;
        double relativeAngle = absoluteAngle - HORIZONTAL_FIELD_OF_VIEW / 2.0;

        System.out.println("Relative Angle from robot to target (degrees): " + relativeAngle);

        double distanceCalculationFRC = TARGET_HEIGHT_INCHES * IMG_HEIGHT
                / (2 * averageHeight * Math.tan((VERTICAL_FIELD_OF_VIEW / 2) * DEGREES_TO_RADIANS));

        System.out.println("Distance from robot to target (inches): " + distanceCalculationFRC);

        // experimental code to find angle of target with respect to robot
        double[] widths;
        widths = table.getNumberArray("width", new double[] { -1 });
        double averageWtH = -1.0;
        if (widths.length >= 2)
        {
            averageWtH = ((widths[0] / heights[0]) + (widths[1] / heights[2])) / 2.0;
        }
        else if (centers.length == 1)
        {
            averageWtH = (widths[0] / heights[0]);
        }
        else
        {
            // do nothing
        }
        System.out.println("Average width to height ratio: " + averageWtH);

        // rough calculation
        double roughAngleMeasurement = averageWtH / TARGET_W_T_H_NORMAL;

        double amountToTurn = (1 - roughAngleMeasurement) * HORIZONTAL_FIELD_OF_VIEW;

        if (amountToTurn < MINIMUM_TURN_ANGLE)
        {
            amountToTurn = 0.0;
        }

        System.out.println("!!NathanDrive!! turn: " + amountToTurn);

        System.out.println("Turn: " + turn + " CenterX: " + centerX);
        if (joy1.getTrigger())
        {
            driveTrain.arcadeDrive(-.75, turn * .005);
        }
        else
        {
            driveTrain.tankDrive(joy1.getY(), joy2.getY());
        }

    }

    public void teleopDisabled()
    {

    }

    /**
     * This function is called periodically during test mode
     */
    @Override
    public void testPeriodic()
    {
    }
}
