package com.aicodingmentor.problem.seed;

import com.aicodingmentor.problem.domain.Problem;
import com.aicodingmentor.problem.domain.ProblemDifficulty;
import com.aicodingmentor.problem.domain.TestCase;
import com.aicodingmentor.problem.repository.ProblemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
public class ProblemDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProblemDataInitializer.class);

    private final ProblemRepository problemRepository;

    public ProblemDataInitializer(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (problemRepository.count() > 0) {
            log.info("Coding problems already initialized. Skipping seed data.");
            return;
        }

        log.info("Seeding initial coding problems...");

        // 1. Two Sum
        Problem twoSum = new Problem(
                "Two Sum",
                "two-sum",
                "Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice.\n\nYou can return the answer in any order.",
                ProblemDifficulty.EASY,
                "2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9\n-10^9 <= target <= 10^9\nOnly one valid answer exists.",
                "First line contains integer array nums\nSecond line contains target integer",
                "Array of two indices [i, j]",
                "Example 1:\nInput: nums = [2,7,11,15], target = 9\nOutput: [0,1]\nExplanation: Because nums[0] + nums[1] == 9, we return [0, 1].\n\nExample 2:\nInput: nums = [3,2,4], target = 6\nOutput: [1,2]",
                "class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // Write your solution here\n        return new int[]{};\n    }\n}",
                "JAVA",
                Set.of("ARRAY", "HASH_TABLE")
        );
        twoSum.addTestCase(new TestCase("[2, 7, 11, 15]\n9", "[0, 1]", false, 1, "nums[0] + nums[1] = 9"));
        twoSum.addTestCase(new TestCase("[3, 2, 4]\n6", "[1, 2]", false, 2, "nums[1] + nums[2] = 6"));
        twoSum.addTestCase(new TestCase("[3, 3]\n6", "[0, 1]", true, 3, null));
        twoSum.addTestCase(new TestCase("[-1, -2, -3, -4, -5]\n-8", "[2, 4]", true, 4, null));
        problemRepository.save(twoSum);

        // 2. Valid Palindrome
        Problem validPalindrome = new Problem(
                "Valid Palindrome",
                "valid-palindrome",
                "A phrase is a **palindrome** if, after converting all uppercase letters into lowercase letters and removing all non-alphanumeric characters, it reads the same forward and backward. Alphanumeric characters include letters and numbers.\n\nGiven a string `s`, return `true` if it is a palindrome, or `false` otherwise.",
                ProblemDifficulty.EASY,
                "1 <= s.length <= 2 * 10^5\ns consists only of printable ASCII characters.",
                "Single string s",
                "Boolean true or false",
                "Example 1:\nInput: s = \"A man, a plan, a canal: Panama\"\nOutput: true\nExplanation: \"amanaplanacanalpanama\" is a palindrome.\n\nExample 2:\nInput: s = \"race a car\"\nOutput: false\nExplanation: \"raceacar\" is not a palindrome.",
                "class Solution {\n    public boolean isPalindrome(String s) {\n        // Write your solution here\n        return false;\n    }\n}",
                "JAVA",
                Set.of("STRING", "TWO_POINTERS")
        );
        validPalindrome.addTestCase(new TestCase("\"A man, a plan, a canal: Panama\"", "true", false, 1, "Reads same forwards and backwards after cleaning"));
        validPalindrome.addTestCase(new TestCase("\"race a car\"", "false", false, 2, "Not a palindrome"));
        validPalindrome.addTestCase(new TestCase("\" \"", "true", true, 3, "Empty string after non-alphanumeric strip is palindrome"));
        validPalindrome.addTestCase(new TestCase("\"0P\"", "false", true, 4, null));
        problemRepository.save(validPalindrome);

        // 3. Contains Duplicate
        Problem containsDuplicate = new Problem(
                "Contains Duplicate",
                "contains-duplicate",
                "Given an integer array `nums`, return `true` if any value appears **at least twice** in the array, and return `false` if every element is distinct.",
                ProblemDifficulty.EASY,
                "1 <= nums.length <= 10^5\n-10^9 <= nums[i] <= 10^9",
                "Integer array nums",
                "Boolean true or false",
                "Example 1:\nInput: nums = [1,2,3,1]\nOutput: true\n\nExample 2:\nInput: nums = [1,2,3,4]\nOutput: false",
                "class Solution {\n    public boolean containsDuplicate(int[] nums) {\n        // Write your solution here\n        return false;\n    }\n}",
                "JAVA",
                Set.of("ARRAY", "HASH_SET")
        );
        containsDuplicate.addTestCase(new TestCase("[1, 2, 3, 1]", "true", false, 1, "Element 1 appears twice"));
        containsDuplicate.addTestCase(new TestCase("[1, 2, 3, 4]", "false", false, 2, "All elements are distinct"));
        containsDuplicate.addTestCase(new TestCase("[1, 1, 1, 3, 3, 4, 3, 2, 4, 2]", "true", true, 3, null));
        problemRepository.save(containsDuplicate);

        // 4. Valid Anagram
        Problem validAnagram = new Problem(
                "Valid Anagram",
                "valid-anagram",
                "Given two strings `s` and `t`, return `true` if `t` is an **anagram** of `s`, and `false` otherwise.\n\nAn **anagram** is a word or phrase formed by rearranging the letters of a different word or phrase, typically using all the original letters exactly once.",
                ProblemDifficulty.EASY,
                "1 <= s.length, t.length <= 5 * 10^4\ns and t consist of lowercase English letters.",
                "First line contains string s\nSecond line contains string t",
                "Boolean true or false",
                "Example 1:\nInput: s = \"anagram\", t = \"nagaram\"\nOutput: true\n\nExample 2:\nInput: s = \"rat\", t = \"car\"\nOutput: false",
                "class Solution {\n    public boolean isAnagram(String s, String t) {\n        // Write your solution here\n        return false;\n    }\n}",
                "JAVA",
                Set.of("STRING", "HASH_TABLE", "SORTING")
        );
        validAnagram.addTestCase(new TestCase("\"anagram\"\n\"nagaram\"", "true", false, 1, "Same character counts"));
        validAnagram.addTestCase(new TestCase("\"rat\"\n\"car\"", "false", false, 2, "Different character sets"));
        validAnagram.addTestCase(new TestCase("\"a\"\n\"ab\"", "false", true, 3, null));
        problemRepository.save(validAnagram);

        // 5. Best Time to Buy and Sell Stock
        Problem buySellStock = new Problem(
                "Best Time to Buy and Sell Stock",
                "best-time-to-buy-and-sell-stock",
                "You are given an array `prices` where `prices[i]` is the price of a given stock on the `i`th day.\n\nYou want to maximize your profit by choosing a **single day** to buy one stock and choosing a **different day in the future** to sell that stock.\n\nReturn the maximum profit you can achieve from this transaction. If you cannot achieve any profit, return `0`.",
                ProblemDifficulty.EASY,
                "1 <= prices.length <= 10^5\n0 <= prices[i] <= 10^4",
                "Integer array prices",
                "Integer maximum profit",
                "Example 1:\nInput: prices = [7,1,5,3,6,4]\nOutput: 5\nExplanation: Buy on day 2 (price = 1) and sell on day 5 (price = 6), profit = 6-1 = 5.\n\nExample 2:\nInput: prices = [7,6,4,3,1]\nOutput: 0\nExplanation: In this case, no transactions are done and the max profit = 0.",
                "class Solution {\n    public int maxProfit(int[] prices) {\n        // Write your solution here\n        return 0;\n    }\n}",
                "JAVA",
                Set.of("ARRAY", "DYNAMIC_PROGRAMMING")
        );
        buySellStock.addTestCase(new TestCase("[7, 1, 5, 3, 6, 4]", "5", false, 1, "Buy at 1, sell at 6"));
        buySellStock.addTestCase(new TestCase("[7, 6, 4, 3, 1]", "0", false, 2, "Prices monotonically decreasing"));
        buySellStock.addTestCase(new TestCase("[2, 4, 1]", "2", true, 3, null));
        problemRepository.save(buySellStock);

        // 6. Maximum Subarray
        Problem maxSubarray = new Problem(
                "Maximum Subarray",
                "maximum-subarray",
                "Given an integer array `nums`, find the subarray with the largest sum, and return *its sum*.",
                ProblemDifficulty.MEDIUM,
                "1 <= nums.length <= 10^5\n-10^4 <= nums[i] <= 10^4",
                "Integer array nums",
                "Integer largest subarray sum",
                "Example 1:\nInput: nums = [-2,1,-3,4,-1,2,1,-5,4]\nOutput: 6\nExplanation: The subarray [4,-1,2,1] has the largest sum 6.\n\nExample 2:\nInput: nums = [1]\nOutput: 1\n\nExample 3:\nInput: nums = [5,4,-1,7,8]\nOutput: 23",
                "class Solution {\n    public int maxSubArray(int[] nums) {\n        // Write your solution here (Kadane's algorithm)\n        return 0;\n    }\n}",
                "JAVA",
                Set.of("ARRAY", "DIVIDE_AND_CONQUER", "DYNAMIC_PROGRAMMING")
        );
        maxSubarray.addTestCase(new TestCase("[-2, 1, -3, 4, -1, 2, 1, -5, 4]", "6", false, 1, "Subarray [4,-1,2,1] sum is 6"));
        maxSubarray.addTestCase(new TestCase("[1]", "1", false, 2, "Single element"));
        maxSubarray.addTestCase(new TestCase("[5, 4, -1, 7, 8]", "23", true, 3, null));
        maxSubarray.addTestCase(new TestCase("[-1]", "-1", true, 4, null));
        problemRepository.save(maxSubarray);

        // 7. Reverse String
        Problem reverseString = new Problem(
                "Reverse String",
                "reverse-string",
                "Write a function that reverses a string. The input string is given as an array of characters `s`.\n\nYou must do this by modifying the input array in-place with `O(1)` extra memory.",
                ProblemDifficulty.EASY,
                "1 <= s.length <= 10^5\ns[i] is a printable ASCII character.",
                "Character array s",
                "Reversed character array",
                "Example 1:\nInput: s = [\"h\",\"e\",\"l\",\"l\",\"o\"]\nOutput: [\"o\",\"l\",\"l\",\"e\",\"h\"]\n\nExample 2:\nInput: s = [\"H\",\"a\",\"n\",\"n\",\"a\",\"h\"]\nOutput: [\"h\",\"a\",\"n\",\"n\",\"a\",\"H\"]",
                "class Solution {\n    public void reverseString(char[] s) {\n        // Write your in-place solution here\n    }\n}",
                "JAVA",
                Set.of("TWO_POINTERS", "STRING")
        );
        reverseString.addTestCase(new TestCase("[\"h\",\"e\",\"l\",\"l\",\"o\"]", "[\"o\",\"l\",\"l\",\"e\",\"h\"]", false, 1, "Reverses hello"));
        reverseString.addTestCase(new TestCase("[\"H\",\"a\",\"n\",\"n\",\"a\",\"h\"]", "[\"h\",\"a\",\"n\",\"n\",\"a\",\"H\"]", false, 2, "Reverses Hannah"));
        problemRepository.save(reverseString);

        // 8. Fizz Buzz
        Problem fizzBuzz = new Problem(
                "Fizz Buzz",
                "fizz-buzz",
                "Given an integer `n`, return a string array `answer` (1-indexed) where:\n- `answer[i] == \"FizzBuzz\"` if `i` is divisible by `3` and `5`.\n- `answer[i] == \"Fizz\"` if `i` is divisible by `3`.\n- `answer[i] == \"Buzz\"` if `i` is divisible by `5`.\n- `answer[i] == i` (as a string) if none of the above conditions are true.",
                ProblemDifficulty.EASY,
                "1 <= n <= 10^4",
                "Integer n",
                "String array of representations from 1 to n",
                "Example 1:\nInput: n = 3\nOutput: [\"1\",\"2\",\"Fizz\"]\n\nExample 2:\nInput: n = 5\nOutput: [\"1\",\"2\",\"Fizz\",\"4\",\"Buzz\"]\n\nExample 3:\nInput: n = 15\nOutput: [\"1\",\"2\",\"Fizz\",\"4\",\"Buzz\",\"Fizz\",\"7\",\"8\",\"Fizz\",\"Buzz\",\"11\",\"Fizz\",\"13\",\"14\",\"FizzBuzz\"]",
                "class Solution {\n    public List<String> fizzBuzz(int n) {\n        // Write your solution here\n        return List.of();\n    }\n}",
                "JAVA",
                Set.of("MATH", "STRING", "SIMULATION")
        );
        fizzBuzz.addTestCase(new TestCase("3", "[\"1\", \"2\", \"Fizz\"]", false, 1, "1 to 3"));
        fizzBuzz.addTestCase(new TestCase("5", "[\"1\", \"2\", \"Fizz\", \"4\", \"Buzz\"]", false, 2, "1 to 5"));
        fizzBuzz.addTestCase(new TestCase("15", "[\"1\", \"2\", \"Fizz\", \"4\", \"Buzz\", \"Fizz\", \"7\", \"8\", \"Fizz\", \"Buzz\", \"11\", \"Fizz\", \"13\", \"14\", \"FizzBuzz\"]", true, 3, null));
        problemRepository.save(fizzBuzz);

        log.info("Successfully seeded 8 coding problems with public and hidden test suites.");
    }
}
