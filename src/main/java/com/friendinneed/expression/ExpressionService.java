package com.friendinneed.expression;

import com.friendinneed.emotion.EmotionLabel;
import com.friendinneed.robot.RobotCommandQueue;
import org.springframework.stereotype.Service;

@Service
public class ExpressionService {
    private final RobotCommandQueue robot;

    public ExpressionService(RobotCommandQueue robot) {
        this.robot = robot;
    }

    public ExpressionCommand commandForEmotion(EmotionLabel e) {
        return switch (e) {
            case JOY -> new ExpressionCommand("HAPPY", .8, null, 600);
            case SADNESS -> new ExpressionCommand("SAD", .7, null, 600);
            case SURPRISE -> new ExpressionCommand("SURPRISED", .8, null, 600);
            case ANGER -> new ExpressionCommand("NEUTRAL", .9, null, 600);
            default -> new ExpressionCommand("NEUTRAL", .5, null, 400);
        };
    }

    public ExpressionCommand commandForState(String state) {
        switch (state) {
            case "HAPPY" -> {
                robot.moveHead(0, 10);
                robot.setLed("#00ff00", "breathe");
            }
            case "SAD" -> {
                robot.moveHead(0, -15);
                robot.setLed("#0088ff", "pulse");
            }
            case "THINKING" -> {
                robot.moveHead(-20, 0);
                robot.setLed("#ffff00", "solid");
            }
            case "LISTENING" -> {
                robot.moveHead(0, 5);
                robot.setLed("#0088ff", "spin");
            }
            case "SPEAKING" -> {
                robot.moveHead((Math.random() - .5) * 10, 4);
                robot.setLed("#00ff00", "pulse");
            }
            default -> {
            }
        }
        return new ExpressionCommand(state, .6, null, 300);
    }
}
