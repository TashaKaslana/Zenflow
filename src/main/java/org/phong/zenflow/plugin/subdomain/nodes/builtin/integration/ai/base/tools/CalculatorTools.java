package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools implements AiToolProvider {
    
    @Tool(description = "Add two numbers together")
    public double add(double a, double b) {
        return a + b;
    }
    
    @Tool(description = "Subtract second number from first number")
    public double subtract(double a, double b) {
        return a - b;
    }
    
    @Tool(description = "Multiply two numbers together")
    public double multiply(double a, double b) {
        return a * b;
    }
    
    @Tool(description = "Divide first number by second number")
    public String divide(double a, double b) {
        if (b == 0) {
            return "Error: Division by zero";
        }
        return String.valueOf(a / b);
    }
    
    @Tool(description = "Calculate a number raised to a power (a^b)")
    public double power(double base, double exponent) {
        return Math.pow(base, exponent);
    }
    
    @Tool(description = "Calculate the square root of a number")
    public String sqrt(double number) {
        if (number < 0) {
            return "Error: Cannot calculate square root of negative number";
        }
        return String.valueOf(Math.sqrt(number));
    }
    
    @Tool(description = "Calculate what percent is 'part' of 'whole'")
    public double percentage(double part, double whole) {
        if (whole == 0) {
            return 0;
        }
        return (part / whole) * 100;
    }
}
