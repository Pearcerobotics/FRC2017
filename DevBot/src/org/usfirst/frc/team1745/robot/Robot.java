package org.usfirst.frc.team1745.robot;

import com.ctre.CANTalon;
import com.ctre.CANTalon.TalonControlMode;

import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.RobotDrive.MotorType;
import edu.wpi.first.wpilibj.Ultrasonic;
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
    double lastTime;
    final String defaultAuto = "Default";
    final String customAuto = "My Auto";
    String autoSelected;
    SendableChooser<String> chooser = new SendableChooser<>();
    CANTalon LFDrive, RFDrive, LBDrive, RBDrive;
    RobotDrive driveTrain;
    Joystick joy1, joy2;
    Ultrasonic ultrasonic;
    boolean firstTime = true;

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
        chooser.addObject("pos1", "pos1");
        chooser.addObject("pos2", "pos2");
        chooser.addObject("pos3", "pos3");
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
        ultrasonic = new Ultrasonic(0, 1);
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
        lastTime = System.currentTimeMillis();
    }

    @Override
    public void teleopInit()
    {
        ultrasonic.setAutomaticMode(true);
    }

    /**
     * This function is called periodically during autonomous
     */
    @Override
    public void autonomousPeriodic()
    {
        double[] heights, centers;
        heights = table.getNumberArray("height", new double[]
        { -1 });
        centers = table.getNumberArray("centerX", new double[]
        { -1 });
        double centerX = 320;
        double distance = 0;

        double centerLeft = 320;
        double centerRight = 320;

        double heightLeft = 320;
        double heightRight = 320;

        if (centers.length >= 2 && heights.length >= 2)
        {
            SmartDashboard.putBoolean("Two Rectangles Seen", true);
            centerX = (centers[0] + centers[1]) / 2;
            distance = 3500 / ((heights[0] + heights[1]) / 2);
            System.out.println("Distance: " + distance);

            if (centers[0] < centers[1])
            {
                centerLeft = centers[0];
                centerRight = centers[1];
                heightLeft = heights[0];
                heightRight = heights[1];
            }
            else
            {
                centerLeft = centers[1];
                centerRight = centers[0];
                heightLeft = heights[1];
                heightRight = heights[0];
            }

        }
        else
        {
            SmartDashboard.putBoolean("Two Rectangles Seen", false);
        }
        if (centers.length == 1 && heights.length == 1)
        {
            centerX = centers[0];
            distance = 3500 / heights[0];
            System.out.println("Distance: " + distance);
        }

        // use when theres only one rectangle
        double turn = (centerX - IMG_WIDTH / 2);

        double sensorDistanceInches = ultrasonic.getRangeInches();
        System.out.println("sensorDistanceInches: " + sensorDistanceInches);

        double turnMod = 0.0;

        int maxTurn = Math.max(125, (int) sensorDistanceInches * 3);
        int minTurn = -maxTurn;

        System.out.println("centerLeft: " + centerLeft);
        System.out.println("centerRight: " + centerRight);
        System.out.println("heightLeft: " + heightLeft);
        System.out.println("heightRight: " + heightRight);

        // else find the farther rectangle
        if (heightLeft > heightRight)
        {
            double ratio = heightLeft / heightRight;

            turnMod = 20 * ratio;

            System.out.println("ratio: " + ratio);

            // Left side is closer, we should aim for the right side
            turn = ((centerX + (int) turnMod) - IMG_WIDTH / 2);

            System.out.println("aiming to the right with a turn mod of " + turnMod);
        }
        else if (heightRight > heightLeft)
        {
            double ratio = heightRight / heightLeft;
            turnMod = 10 * ratio;

            System.out.println("ratio: " + ratio);
            // Right side is closer, we should aim for the left side
            turn = ((centerX - (int) turnMod) - IMG_WIDTH / 2);

            System.out.println("aiming to the left with a turn mod of " + turnMod);
        }
        else
        {
            // sides are either the same or we only have rectangle

            turn = (centerX - IMG_WIDTH / 2);
            System.out.println("going straight ahead or only see one rectangle: ");
        }

        // turn = (centerRight - (IMG_WIDTH / 2));

        System.out.println("original turn: " + turn);

        turn = Math.min(maxTurn, turn);
        turn = Math.max(minTurn, turn);

        if (sensorDistanceInches < 30)
        {
            turn = 0;
        }

        boolean youShallNotPass = false;
        if (sensorDistanceInches < 15)
        {
            youShallNotPass = true;
        }
        if (autoSelected.equals("pos1"))
        {
            if (System.currentTimeMillis() - lastTime < 4000)
            {
                driveTrain.arcadeDrive(-.55, 0);
            }
            else if (System.currentTimeMillis() - lastTime < 4500)
            {
                driveTrain.arcadeDrive(-.3, .7);
            }
            else if (System.currentTimeMillis() - lastTime < 7500)
            {
                driveTrain.arcadeDrive(-.55, turn * .005);
            }
            else
            {
                if (!youShallNotPass)
                {
                    driveTrain.arcadeDrive(-.55, turn * .005);
                }
                else
                {
                    driveTrain.arcadeDrive(0, 0);
                }
            }
        }
        else if (autoSelected.equals("pos2"))
        {
            if (System.currentTimeMillis() - lastTime < 3500)
            {
                driveTrain.arcadeDrive(-.55, 0);
            }
            else if (System.currentTimeMillis() - lastTime < 4500)
            {
                driveTrain.arcadeDrive(-.3, -.7);
            }
            else if (System.currentTimeMillis() - lastTime < 7500)
            {
                driveTrain.arcadeDrive(-.55, turn * .005);
            }
            else
            {
                if (!youShallNotPass)
                {
                    driveTrain.arcadeDrive(-.55, turn * .005);
                }
                else
                {
                    driveTrain.arcadeDrive(0, 0);
                }
            }
        }
        else if (autoSelected.equals("pos3"))
        {
            if (System.currentTimeMillis() - lastTime < 1000)
            {
                driveTrain.arcadeDrive(-.55, 0);
            }
            else
            {
                if (!youShallNotPass)
                {
                    driveTrain.arcadeDrive(-.55, turn * .005);
                }
                else
                {
                    driveTrain.arcadeDrive(0, 0);
                }
            }
        }

    }

    /**
     * This function is called periodically during operator control
     */
    @Override
    public void teleopPeriodic()
    {
        double[] heights, centers;
        heights = table.getNumberArray("height", new double[]
        { -1 });
        centers = table.getNumberArray("centerX", new double[]
        { -1 });
        double centerX = 320;
        double distance = 0;

        double centerLeft = 320;
        double centerRight = 320;

        double heightLeft = 320;
        double heightRight = 320;

        if (centers.length >= 2 && heights.length >= 2)
        {
            SmartDashboard.putBoolean("Two Rectangles Seen", true);
            centerX = (centers[0] + centers[1]) / 2;
            distance = 3500 / ((heights[0] + heights[1]) / 2);
            System.out.println("Distance: " + distance);

            if (centers[0] < centers[1])
            {
                centerLeft = centers[0];
                centerRight = centers[1];
                heightLeft = heights[0];
                heightRight = heights[1];
            }
            else
            {
                centerLeft = centers[1];
                centerRight = centers[0];
                heightLeft = heights[1];
                heightRight = heights[0];
            }

        }
        else
        {
            SmartDashboard.putBoolean("Two Rectangles Seen", false);
        }
        if (centers.length == 1 && heights.length == 1)
        {
            centerX = centers[0];
            distance = 3500 / heights[0];
            System.out.println("Distance: " + distance);
        }

        // use when theres only one rectangle
        double turn = (centerX - IMG_WIDTH / 2);

        double sensorDistanceInches = ultrasonic.getRangeInches();
        System.out.println("sensorDistanceInches: " + sensorDistanceInches);

        double turnMod = 0.0;

        int maxTurn = Math.max(125, (int) sensorDistanceInches * 3);
        int minTurn = -maxTurn;

        System.out.println("centerLeft: " + centerLeft);
        System.out.println("centerRight: " + centerRight);
        System.out.println("heightLeft: " + heightLeft);
        System.out.println("heightRight: " + heightRight);

        // else find the farther rectangle
        if (heightLeft > heightRight)
        {
            double ratio = heightLeft / heightRight;

            turnMod = 20 * ratio;

            System.out.println("ratio: " + ratio);

            // Left side is closer, we should aim for the right side
            turn = ((centerX + (int) turnMod) - IMG_WIDTH / 2);

            System.out.println("aiming to the right with a turn mod of " + turnMod);
        }
        else if (heightRight > heightLeft)
        {
            double ratio = heightRight / heightLeft;
            turnMod = 10 * ratio;

            System.out.println("ratio: " + ratio);
            // Right side is closer, we should aim for the left side
            turn = ((centerX - (int) turnMod) - IMG_WIDTH / 2);

            System.out.println("aiming to the left with a turn mod of " + turnMod);
        }
        else
        {
            // sides are either the same or we only have rectangle

            turn = (centerX - IMG_WIDTH / 2);
            System.out.println("going straight ahead or only see one rectangle: ");
        }

        // turn = (centerRight - (IMG_WIDTH / 2));

        System.out.println("original turn: " + turn);

        turn = Math.min(maxTurn, turn);
        turn = Math.max(minTurn, turn);

        if (sensorDistanceInches < 30)
        {
            turn = 0;
        }

        boolean youShallNotPass = false;
        if (sensorDistanceInches < 11)
        {
            youShallNotPass = true;
        }

        //
        //
        // // Code from Nathan to determine distance and angle to the target
        // // constants
        // final double HORIZONTAL_FIELD_OF_VIEW = 55.6496; // degrees (also
        // found
        // // 59.7, we should
        // // calibrate)
        // //Also found 55.6496
        // final double VERTICAL_FIELD_OF_VIEW = 37.84; //39.3072; // degrees
        // TODO calibrate
        // final double DEGREES_TO_RADIANS = 0.01745329252;
        // final double RADIANS_TO_DEGREES = 57.295779513;
        // final double TARGET_HEIGHT_INCHES = 5.0; // inches
        // final double TARGET_WIDTH_INCHES = 2.0; // inches
        // final double TARGET_W_T_H_NORMAL = TARGET_WIDTH_INCHES /
        // TARGET_HEIGHT_INCHES;
        // final double MINIMUM_TURN_ANGLE = 5.0; // degrees
        //
        // // calculations
        // double averageCenter = 0.0;
        // double averageHeight = 0.0;
        //
        // // Get average center and height
        // if (centers.length >= 2 && heights.length >= 2)
        // {
        // averageCenter = (centers[0] + centers[1]) / 2.0;
        // averageHeight = (heights[0] + heights[1]) / 2.0;
        // }
        // else if (centers.length == 1 && heights.length == 1)
        // {
        // averageCenter = centers[0];
        // averageHeight = heights[0];
        // }
        // else
        // {
        // // do nothing
        // }
        //
        // double absoluteAngle = averageCenter / IMG_WIDTH *
        // HORIZONTAL_FIELD_OF_VIEW;
        // double relativeAngle = absoluteAngle - HORIZONTAL_FIELD_OF_VIEW /
        // 2.0;
        //
        //// System.out.println("Relative Angle from robot to target (degrees):
        // " + relativeAngle);
        //
        // double distanceCalculationFRC = TARGET_HEIGHT_INCHES * IMG_HEIGHT
        // / (2 * averageHeight * Math.tan((VERTICAL_FIELD_OF_VIEW / 2) *
        // DEGREES_TO_RADIANS));
        //
        //// System.out.println("Distance from robot to target (inches): " +
        // distanceCalculationFRC);
        //
        // // experimental code to find angle of target with respect to robot
        // double[] widths;
        // widths = table.getNumberArray("width", new double[] { -1 });
        // double averageWtH = -1.0;
        //
        //
        //
        // if (widths.length >= 2 && heights.length >= 2)
        // {
        // averageWtH = ((widths[0] / heights[0]) + (widths[1] / heights[1])) /
        // 2.0;
        // }
        // else if (widths.length == 1 && heights.length ==1 )
        // {
        // averageWtH = (widths[0] / heights[0]);
        // }
        // else
        // {
        // // do nothing
        // }
        //// System.out.println("Average width to height ratio: " + averageWtH);
        //
        // // rough calculation
        // double roughAngleMeasurement = averageWtH / TARGET_W_T_H_NORMAL;
        //
        // double amountToTurn = (1 - roughAngleMeasurement) *
        // HORIZONTAL_FIELD_OF_VIEW;
        //
        // if (amountToTurn < MINIMUM_TURN_ANGLE)
        // {
        // amountToTurn = 0.0;
        // }

        // System.out.println("!!NathanDrive!! turn: " + amountToTurn);

        // System.out.println("Turn: " + turn + " actualTurn: " + (turn *
        // 0.005)*RADIANS_TO_DEGREES);
        if (joy1.getTrigger())
        {
            if (!youShallNotPass)
            {
                driveTrain.arcadeDrive(-.55, turn * .005);
            }
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