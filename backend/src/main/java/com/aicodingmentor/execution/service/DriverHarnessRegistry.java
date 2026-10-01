package com.aicodingmentor.execution.service;

import com.aicodingmentor.execution.model.SupportedLanguage;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class DriverHarnessRegistry {

    public record ProblemTemplate(
            String starterCode,
            String driverHarness
    ) {}

    private final Map<String, Map<SupportedLanguage, ProblemTemplate>> templatesBySlug = new HashMap<>();

    public DriverHarnessRegistry() {
        registerTwoSum();
        registerValidPalindrome();
        registerContainsDuplicate();
        registerReverseString();
    }

    public ProblemTemplate getTemplate(String slug, SupportedLanguage language) {
        if (slug == null) {
            return getDefaultTemplate(language);
        }
        Map<SupportedLanguage, ProblemTemplate> langMap = templatesBySlug.get(slug.toLowerCase());
        if (langMap == null || !langMap.containsKey(language)) {
            return getDefaultTemplate(language);
        }
        return langMap.get(language);
    }

    public Map<String, String> getAllStarterCodes(String slug) {
        Map<String, String> result = new HashMap<>();
        for (SupportedLanguage lang : SupportedLanguage.values()) {
            ProblemTemplate template = getTemplate(slug, lang);
            result.put(lang.name(), template.starterCode());
        }
        return Collections.unmodifiableMap(result);
    }

    private ProblemTemplate getDefaultTemplate(SupportedLanguage language) {
        return switch (language) {
            case PYTHON -> new ProblemTemplate(
                    "class Solution:\n    # Write your solution here\n    pass\n",
                    "{{USER_CODE}}\n"
            );
            case CPP -> new ProblemTemplate(
                    "#include <iostream>\n#include <vector>\nusing namespace std;\n\nclass Solution {\npublic:\n    // Write your solution here\n};\n",
                    "{{USER_CODE}}\n"
            );
            case JAVASCRIPT -> new ProblemTemplate(
                    "/**\n * Write your solution here\n */\n",
                    "{{USER_CODE}}\n"
            );
            case JAVA -> new ProblemTemplate(
                    "class Solution {\n    // Write your solution here\n}\n",
                    "{{USER_CODE}}\n"
            );
        };
    }

    private void registerTwoSum() {
        Map<SupportedLanguage, ProblemTemplate> map = new HashMap<>();

        // 1. JAVA
        map.put(SupportedLanguage.JAVA, new ProblemTemplate(
                """
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        // Write your solution here
                        return new int[]{};
                    }
                }
                """,
                """
                import java.util.*;

                {{USER_CODE}}

                public class MainDriver {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextLine()) return;
                        String line = sc.nextLine().trim().replace("[", "").replace("]", "").trim();
                        String[] tokens = line.split("\\s*,\\s*|\\s+");
                        int[] nums = new int[tokens.length];
                        for (int i = 0; i < tokens.length; i++) {
                            nums[i] = Integer.parseInt(tokens[i]);
                        }
                        int target = sc.nextInt();
                        int[] res = new Solution().twoSum(nums, target);
                        System.out.println(Arrays.toString(res));
                    }
                }
                """
        ));

        // 2. PYTHON
        map.put(SupportedLanguage.PYTHON, new ProblemTemplate(
                """
                class Solution:
                    def twoSum(self, nums: list[int], target: int) -> list[int]:
                        # Write your solution here
                        return []
                """,
                """
                import sys, json

                {{USER_CODE}}

                if __name__ == '__main__':
                    input_data = sys.stdin.read().strip().split('\\n')
                    if input_data and input_data[0]:
                        nums = json.loads(input_data[0].strip())
                        target = int(input_data[1].strip())
                        result = Solution().twoSum(nums, target)
                        print(json.dumps(result))
                """
        ));

        // 3. JAVASCRIPT
        map.put(SupportedLanguage.JAVASCRIPT, new ProblemTemplate(
                """
                /**
                 * @param {number[]} nums
                 * @param {number} target
                 * @return {number[]}
                 */
                var twoSum = function(nums, target) {
                    // Write your solution here
                    return [];
                };
                """,
                """
                const fs = require('fs');

                {{USER_CODE}}

                const input = fs.readFileSync(0, 'utf-8').trim().split('\\n');
                if (input.length >= 2) {
                    const nums = JSON.parse(input[0].trim());
                    const target = parseInt(input[1].trim(), 10);
                    const result = twoSum(nums, target);
                    console.log(JSON.stringify(result));
                }
                """
        ));

        // 4. C++
        map.put(SupportedLanguage.CPP, new ProblemTemplate(
                """
                #include <vector>
                #include <unordered_map>
                using namespace std;

                class Solution {
                public:
                    vector<int> twoSum(vector<int>& nums, int target) {
                        // Write your solution here
                        return {};
                    }
                };
                """,
                """
                #include <iostream>
                #include <vector>
                #include <string>
                #include <sstream>
                #include <algorithm>
                using namespace std;

                {{USER_CODE}}

                int main() {
                    string line;
                    if (!getline(cin, line)) return 0;
                    line.erase(remove(line.begin(), line.end(), '['), line.end());
                    line.erase(remove(line.begin(), line.end(), ']'), line.end());
                    stringstream ss(line);
                    vector<int> nums;
                    int n;
                    while (ss >> n) {
                        nums.push_back(n);
                        if (ss.peek() == ',' || ss.peek() == ' ') ss.ignore();
                    }
                    int target;
                    if (cin >> target) {
                        Solution sol;
                        vector<int> res = sol.twoSum(nums, target);
                        cout << "[";
                        for (size_t i = 0; i < res.size(); i++) {
                            cout << res[i] << (i + 1 < res.size() ? ", " : "");
                        }
                        cout << "]" << endl;
                    }
                    return 0;
                }
                """
        ));

        templatesBySlug.put("two-sum", map);
    }

    private void registerValidPalindrome() {
        Map<SupportedLanguage, ProblemTemplate> map = new HashMap<>();

        // JAVA
        map.put(SupportedLanguage.JAVA, new ProblemTemplate(
                """
                class Solution {
                    public boolean isPalindrome(String s) {
                        // Write your solution here
                        return false;
                    }
                }
                """,
                """
                import java.util.*;

                {{USER_CODE}}

                public class MainDriver {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextLine()) return;
                        String s = sc.nextLine().trim();
                        if (s.startsWith("\\\"") && s.endsWith("\\\"") && s.length() >= 2) {
                            s = s.substring(1, s.length() - 1);
                        }
                        boolean res = new Solution().isPalindrome(s);
                        System.out.println(res);
                    }
                }
                """
        ));

        // PYTHON
        map.put(SupportedLanguage.PYTHON, new ProblemTemplate(
                """
                class Solution:
                    def isPalindrome(self, s: str) -> bool:
                        # Write your solution here
                        return False
                """,
                """
                import sys, json

                {{USER_CODE}}

                if __name__ == '__main__':
                    s = sys.stdin.read().strip()
                    if s.startswith('"') and s.endswith('"'):
                        s = s[1:-1]
                    result = Solution().isPalindrome(s)
                    print("true" if result else "false")
                """
        ));

        // JAVASCRIPT
        map.put(SupportedLanguage.JAVASCRIPT, new ProblemTemplate(
                """
                /**
                 * @param {string} s
                 * @return {boolean}
                 */
                var isPalindrome = function(s) {
                    // Write your solution here
                    return false;
                };
                """,
                """
                const fs = require('fs');

                {{USER_CODE}}

                let s = fs.readFileSync(0, 'utf-8').trim();
                if (s.startsWith('"') && s.endsWith('"')) {
                    s = s.slice(1, -1);
                }
                const result = isPalindrome(s);
                console.log(result ? "true" : "false");
                """
        ));

        // C++
        map.put(SupportedLanguage.CPP, new ProblemTemplate(
                """
                #include <string>
                #include <cctype>
                using namespace std;

                class Solution {
                public:
                    bool isPalindrome(string s) {
                        // Write your solution here
                        return false;
                    }
                };
                """,
                """
                #include <iostream>
                #include <string>
                using namespace std;

                {{USER_CODE}}

                int main() {
                    string s;
                    if (!getline(cin, s)) return 0;
                    if (s.front() == '"' && s.back() == '"' && s.length() >= 2) {
                        s = s.substr(1, s.length() - 2);
                    }
                    Solution sol;
                    bool res = sol.isPalindrome(s);
                    cout << (res ? "true" : "false") << endl;
                    return 0;
                }
                """
        ));

        templatesBySlug.put("valid-palindrome", map);
    }

    private void registerContainsDuplicate() {
        Map<SupportedLanguage, ProblemTemplate> map = new HashMap<>();

        // JAVA
        map.put(SupportedLanguage.JAVA, new ProblemTemplate(
                """
                class Solution {
                    public boolean containsDuplicate(int[] nums) {
                        // Write your solution here
                        return false;
                    }
                }
                """,
                """
                import java.util.*;

                {{USER_CODE}}

                public class MainDriver {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextLine()) return;
                        String line = sc.nextLine().trim().replace("[", "").replace("]", "").trim();
                        String[] tokens = line.split("\\s*,\\s*|\\s+");
                        int[] nums = new int[tokens.length];
                        for (int i = 0; i < tokens.length; i++) {
                            nums[i] = Integer.parseInt(tokens[i]);
                        }
                        boolean res = new Solution().containsDuplicate(nums);
                        System.out.println(res);
                    }
                }
                """
        ));

        // PYTHON
        map.put(SupportedLanguage.PYTHON, new ProblemTemplate(
                """
                class Solution:
                    def containsDuplicate(self, nums: list[int]) -> bool:
                        # Write your solution here
                        return False
                """,
                """
                import sys, json

                {{USER_CODE}}

                if __name__ == '__main__':
                    line = sys.stdin.read().strip()
                    if line:
                        nums = json.loads(line)
                        result = Solution().containsDuplicate(nums)
                        print("true" if result else "false")
                """
        ));

        // JAVASCRIPT
        map.put(SupportedLanguage.JAVASCRIPT, new ProblemTemplate(
                """
                /**
                 * @param {number[]} nums
                 * @return {boolean}
                 */
                var containsDuplicate = function(nums) {
                    // Write your solution here
                    return false;
                };
                """,
                """
                const fs = require('fs');

                {{USER_CODE}}

                const input = fs.readFileSync(0, 'utf-8').trim();
                if (input) {
                    const nums = JSON.parse(input);
                    const result = containsDuplicate(nums);
                    console.log(result ? "true" : "false");
                }
                """
        ));

        // C++
        map.put(SupportedLanguage.CPP, new ProblemTemplate(
                """
                #include <vector>
                #include <unordered_set>
                using namespace std;

                class Solution {
                public:
                    bool containsDuplicate(vector<int>& nums) {
                        // Write your solution here
                        return false;
                    }
                };
                """,
                """
                #include <iostream>
                #include <vector>
                #include <string>
                #include <sstream>
                #include <algorithm>
                using namespace std;

                {{USER_CODE}}

                int main() {
                    string line;
                    if (!getline(cin, line)) return 0;
                    line.erase(remove(line.begin(), line.end(), '['), line.end());
                    line.erase(remove(line.begin(), line.end(), ']'), line.end());
                    stringstream ss(line);
                    vector<int> nums;
                    int n;
                    while (ss >> n) {
                        nums.push_back(n);
                        if (ss.peek() == ',' || ss.peek() == ' ') ss.ignore();
                    }
                    Solution sol;
                    bool res = sol.containsDuplicate(nums);
                    cout << (res ? "true" : "false") << endl;
                    return 0;
                }
                """
        ));

        templatesBySlug.put("contains-duplicate", map);
    }

    private void registerReverseString() {
        Map<SupportedLanguage, ProblemTemplate> map = new HashMap<>();

        // JAVA
        map.put(SupportedLanguage.JAVA, new ProblemTemplate(
                """
                class Solution {
                    public void reverseString(char[] s) {
                        // Modify s in-place
                    }
                }
                """,
                """
                import java.util.*;

                {{USER_CODE}}

                public class MainDriver {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextLine()) return;
                        String line = sc.nextLine().trim();
                        String clean = line.replaceAll("[\\[\\]\\\"\\s]", "");
                        String[] tokens = clean.split(",");
                        char[] s = new char[tokens.length];
                        for (int i = 0; i < tokens.length; i++) {
                            s[i] = tokens[i].charAt(0);
                        }
                        new Solution().reverseString(s);
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < s.length; i++) {
                            sb.append("\\\"").append(s[i]).append("\\\"");
                            if (i + 1 < s.length) sb.append(", ");
                        }
                        sb.append("]");
                        System.out.println(sb.toString());
                    }
                }
                """
        ));

        // PYTHON
        map.put(SupportedLanguage.PYTHON, new ProblemTemplate(
                """
                class Solution:
                    def reverseString(self, s: list[str]) -> None:
                        # Modify s in-place
                        pass
                """,
                """
                import sys, json

                {{USER_CODE}}

                if __name__ == '__main__':
                    line = sys.stdin.read().strip()
                    if line:
                        s = json.loads(line)
                        Solution().reverseString(s)
                        print(json.dumps(s))
                """
        ));

        // JAVASCRIPT
        map.put(SupportedLanguage.JAVASCRIPT, new ProblemTemplate(
                """
                /**
                 * @param {character[]} s
                 * @return {void} Do not return anything, modify s in-place instead.
                 */
                var reverseString = function(s) {
                    // Modify s in-place
                };
                """,
                """
                const fs = require('fs');

                {{USER_CODE}}

                const input = fs.readFileSync(0, 'utf-8').trim();
                if (input) {
                    const s = JSON.parse(input);
                    reverseString(s);
                    console.log(JSON.stringify(s));
                }
                """
        ));

        // C++
        map.put(SupportedLanguage.CPP, new ProblemTemplate(
                """
                #include <vector>
                using namespace std;

                class Solution {
                public:
                    void reverseString(vector<char>& s) {
                        // Modify s in-place
                    }
                };
                """,
                """
                #include <iostream>
                #include <vector>
                #include <string>
                #include <algorithm>
                using namespace std;

                {{USER_CODE}}

                int main() {
                    string line;
                    if (!getline(cin, line)) return 0;
                    vector<char> s;
                    for (char c : line) {
                        if (c != '[' && c != ']' && c != ',' && c != '"' && c != ' ') {
                            s.push_back(c);
                        }
                    }
                    Solution sol;
                    sol.reverseString(s);
                    cout << "[";
                    for (size_t i = 0; i < s.size(); i++) {
                        cout << "\\"" << s[i] << "\\"" << (i + 1 < s.size() ? ", " : "");
                    }
                    cout << "]" << endl;
                    return 0;
                }
                """
        ));

        templatesBySlug.put("reverse-string", map);
    }
}
