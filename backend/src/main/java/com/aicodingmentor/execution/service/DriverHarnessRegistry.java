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
        registerValidAnagram();
        registerBuySellStock();
        registerMaxSubarray();
        registerReverseString();
        registerFizzBuzz();
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
                    """
                    import sys, json

                    {{USER_CODE}}

                    if __name__ == '__main__':
                        raw = sys.stdin.read().strip()
                        if raw:
                            lines = [l.strip() for l in raw.split('\\n') if l.strip()]
                            sol = Solution()
                            methods = [m for m in dir(sol) if not m.startswith('_') and callable(getattr(sol, m))]
                            if methods:
                                target = getattr(sol, methods[0])
                                import inspect
                                params = list(inspect.signature(target).parameters.values())
                                args = []
                                for idx, p in enumerate(params):
                                    if idx < len(lines):
                                        val = lines[idx]
                                        try:
                                            args.append(json.loads(val))
                                        except Exception:
                                            args.append(val)
                                res = target(*args)
                                if res is not None:
                                    print(json.dumps(res))
                                elif args:
                                    print(json.dumps(args[0]))
                    """
            );
            case CPP -> new ProblemTemplate(
                    "#include <iostream>\n#include <vector>\nusing namespace std;\n\nclass Solution {\npublic:\n    // Write your solution here\n};\n",
                    "{{USER_CODE}}\n"
            );
            case JAVASCRIPT -> new ProblemTemplate(
                    "/**\n * Write your solution here\n */\n",
                    """
                    const fs = require('fs');

                    {{USER_CODE}}

                    const raw = fs.readFileSync(0, 'utf-8').trim();
                    if (raw) {
                        const lines = raw.split('\\n').map(l => l.trim()).filter(Boolean);
                        const fnNames = Object.keys(global).filter(k => typeof global[k] === 'function' && k !== 'clearImmediate' && k !== 'clearInterval' && k !== 'clearTimeout');
                        // Find solution function or invoke directly
                        const args = lines.map(l => {
                            try { return JSON.parse(l); } catch(e) { return l; }
                        });
                        let fn = (typeof solution !== 'undefined' ? solution : null);
                        if (!fn) {
                            for (let key in this) {
                                if (typeof this[key] === 'function') { fn = this[key]; break; }
                            }
                        }
                        if (fn) {
                            const res = fn(...args);
                            if (res !== undefined) {
                                console.log(JSON.stringify(res));
                            } else if (args.length > 0) {
                                console.log(JSON.stringify(args[0]));
                            }
                        }
                    }
                    """
            );
            case JAVA -> new ProblemTemplate(
                    "class Solution {\n    // Write your solution here\n}\n",
                    """
                    import java.util.*;
                    import java.util.stream.*;
                    import java.lang.reflect.*;

                    {{USER_CODE}}

                    class MainDriver {
                        public static void main(String[] args) {
                            try {
                                Scanner sc = new Scanner(System.in);
                                List<String> inputLines = new ArrayList<>();
                                while (sc.hasNextLine()) {
                                    String line = sc.nextLine();
                                    if (!line.trim().isEmpty()) {
                                        inputLines.add(line.trim());
                                    }
                                }

                                Class<?> clazz = Class.forName("Solution");
                                Object instance = clazz.getDeclaredConstructor().newInstance();
                                Method[] methods = clazz.getDeclaredMethods();
                                Method target = null;
                                for (Method m : methods) {
                                    if (Modifier.isPublic(m.getModifiers()) && !m.getName().equals("main")) {
                                        target = m;
                                        break;
                                    }
                                }
                                if (target == null && methods.length > 0) target = methods[0];
                                if (target == null) return;

                                target.setAccessible(true);
                                Class<?>[] paramTypes = target.getParameterTypes();
                                Object[] invokeArgs = new Object[paramTypes.length];

                                for (int i = 0; i < paramTypes.length; i++) {
                                    String line = i < inputLines.size() ? inputLines.get(i) : "";
                                    invokeArgs[i] = parseArgument(line, paramTypes[i]);
                                }

                                Object result = target.invoke(instance, invokeArgs);

                                if (target.getReturnType() == void.class && invokeArgs.length > 0) {
                                    printFormatted(invokeArgs[0]);
                                } else {
                                    printFormatted(result);
                                }
                            } catch (Throwable t) {
                                Throwable cause = t.getCause() != null ? t.getCause() : t;
                                cause.printStackTrace();
                            }
                        }

                        private static Object parseArgument(String raw, Class<?> type) {
                            if (raw == null) return null;
                            String trimmed = raw.trim();

                            if (type == int[].class) {
                                String clean = trimmed.replace("[", "").replace("]", "").trim();
                                if (clean.isEmpty()) return new int[0];
                                String[] tokens = clean.split("\\\\s*,\\\\s*|\\\\s+");
                                int[] arr = new int[tokens.length];
                                for (int i = 0; i < tokens.length; i++) {
                                    arr[i] = Integer.parseInt(tokens[i].trim());
                                }
                                return arr;
                            } else if (type == char[].class) {
                                String clean = trimmed.replace("[", "").replace("]", "").replace("\\\"", "").replace("'", "").trim();
                                if (clean.isEmpty()) return new char[0];
                                String[] tokens = clean.split("\\\\s*,\\\\s*|\\\\s+");
                                char[] arr = new char[tokens.length];
                                for (int i = 0; i < tokens.length; i++) {
                                    arr[i] = tokens[i].trim().charAt(0);
                                }
                                return arr;
                            } else if (type == String[].class) {
                                String clean = trimmed.replace("[", "").replace("]", "").trim();
                                if (clean.isEmpty()) return new String[0];
                                String[] tokens = clean.split("\\\\s*,\\\\s*");
                                String[] arr = new String[tokens.length];
                                for (int i = 0; i < tokens.length; i++) {
                                    arr[i] = tokens[i].replace("\\\"", "").trim();
                                }
                                return arr;
                            } else if (type == int.class || type == Integer.class) {
                                return Integer.parseInt(trimmed);
                            } else if (type == long.class || type == Long.class) {
                                return Long.parseLong(trimmed);
                            } else if (type == double.class || type == Double.class) {
                                return Double.parseDouble(trimmed);
                            } else if (type == boolean.class || type == Boolean.class) {
                                return Boolean.parseBoolean(trimmed);
                            } else if (type == String.class) {
                                if (trimmed.startsWith("\\\"") && trimmed.endsWith("\\\"") && trimmed.length() >= 2) {
                                    return trimmed.substring(1, trimmed.length() - 1);
                                }
                                return trimmed;
                            } else if (List.class.isAssignableFrom(type)) {
                                String clean = trimmed.replace("[", "").replace("]", "").trim();
                                if (clean.isEmpty()) return new ArrayList<>();
                                String[] tokens = clean.split("\\\\s*,\\\\s*");
                                List<String> list = new ArrayList<>();
                                for (String token : tokens) {
                                    list.add(token.replace("\\\"", "").trim());
                                }
                                return list;
                            }
                            return trimmed;
                        }

                        private static void printFormatted(Object obj) {
                            if (obj == null) return;
                            if (obj instanceof int[] arr) {
                                System.out.println(Arrays.toString(arr));
                            } else if (obj instanceof char[] arr) {
                                StringBuilder sb = new StringBuilder("[");
                                for (int i = 0; i < arr.length; i++) {
                                    sb.append("\\\"").append(arr[i]).append("\\\"");
                                    if (i < arr.length - 1) sb.append(", ");
                                }
                                sb.append("]");
                                System.out.println(sb.toString());
                            } else if (obj instanceof Object[] arr) {
                                System.out.println(Arrays.toString(arr));
                            } else if (obj instanceof List<?> list) {
                                StringBuilder sb = new StringBuilder("[");
                                for (int i = 0; i < list.size(); i++) {
                                    Object item = list.get(i);
                                    if (item instanceof String) {
                                        sb.append("\\\"").append(item).append("\\\"");
                                    } else {
                                        sb.append(item);
                                    }
                                    if (i < list.size() - 1) sb.append(", ");
                                }
                                sb.append("]");
                                System.out.println(sb.toString());
                            } else {
                                System.out.println(obj);
                            }
                        }
                    }
                    """
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

                class MainDriver {
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

                class MainDriver {
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

                class MainDriver {
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

                class MainDriver {
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

    private void registerFizzBuzz() {
        Map<SupportedLanguage, ProblemTemplate> map = new HashMap<>();

        // JAVA
        map.put(SupportedLanguage.JAVA, new ProblemTemplate(
                """
                class Solution {
                    public List<String> fizzBuzz(int n) {
                        // Write your solution here
                        return List.of();
                    }
                }
                """,
                """
                import java.util.*;

                {{USER_CODE}}

                class MainDriver {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextInt()) return;
                        int n = sc.nextInt();
                        List<String> res = new Solution().fizzBuzz(n);
                        StringBuilder sb = new StringBuilder("[");
                        for (int i = 0; i < res.size(); i++) {
                            sb.append("\\\"").append(res.get(i)).append("\\\"");
                            if (i < res.size() - 1) sb.append(", ");
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
                    def fizzBuzz(self, n: int) -> list[str]:
                        # Write your solution here
                        return []
                """,
                """
                import sys, json

                {{USER_CODE}}

                if __name__ == '__main__':
                    line = sys.stdin.read().strip()
                    if line:
                        n = int(line)
                        res = Solution().fizzBuzz(n)
                        print(json.dumps(res))
                """
        ));

        // JAVASCRIPT
        map.put(SupportedLanguage.JAVASCRIPT, new ProblemTemplate(
                """
                /**
                 * @param {number} n
                 * @return {string[]}
                 */
                var fizzBuzz = function(n) {
                    // Write your solution here
                    return [];
                };
                """,
                """
                const fs = require('fs');

                {{USER_CODE}}

                const input = fs.readFileSync(0, 'utf-8').trim();
                if (input) {
                    const n = parseInt(input, 10);
                    const res = fizzBuzz(n);
                    console.log(JSON.stringify(res));
                }
                """
        ));

        // C++
        map.put(SupportedLanguage.CPP, new ProblemTemplate(
                """
                #include <vector>
                #include <string>
                using namespace std;

                class Solution {
                public:
                    vector<string> fizzBuzz(int n) {
                        // Write your solution here
                        return {};
                    }
                };
                """,
                """
                #include <iostream>
                #include <vector>
                #include <string>
                using namespace std;

                {{USER_CODE}}

                int main() {
                    int n;
                    if (cin >> n) {
                        Solution sol;
                        vector<string> res = sol.fizzBuzz(n);
                        cout << "[";
                        for (size_t i = 0; i < res.size(); i++) {
                            cout << "\\"" << res[i] << "\\"" << (i + 1 < res.size() ? ", " : "");
                        }
                        cout << "]" << endl;
                    }
                    return 0;
                }
                """
        ));

        templatesBySlug.put("fizz-buzz", map);
    }

    private void registerValidAnagram() {
        Map<SupportedLanguage, ProblemTemplate> map = new HashMap<>();

        // JAVA
        map.put(SupportedLanguage.JAVA, new ProblemTemplate(
                """
                class Solution {
                    public boolean isAnagram(String s, String t) {
                        // Write your solution here
                        return false;
                    }
                }
                """,
                """
                import java.util.*;

                {{USER_CODE}}

                class MainDriver {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextLine()) return;
                        String s = sc.nextLine().trim().replace("\\\"", "");
                        if (!sc.hasNextLine()) return;
                        String t = sc.nextLine().trim().replace("\\\"", "");
                        boolean res = new Solution().isAnagram(s, t);
                        System.out.println(res);
                    }
                }
                """
        ));

        templatesBySlug.put("valid-anagram", map);
    }

    private void registerBuySellStock() {
        Map<SupportedLanguage, ProblemTemplate> map = new HashMap<>();

        // JAVA
        map.put(SupportedLanguage.JAVA, new ProblemTemplate(
                """
                class Solution {
                    public int maxProfit(int[] prices) {
                        // Write your solution here
                        return 0;
                    }
                }
                """,
                """
                import java.util.*;

                {{USER_CODE}}

                class MainDriver {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextLine()) return;
                        String line = sc.nextLine().trim().replace("[", "").replace("]", "").trim();
                        String[] tokens = line.split("\\\\s*,\\\\s*|\\\\s+");
                        int[] prices = new int[tokens.length];
                        for (int i = 0; i < tokens.length; i++) {
                            prices[i] = Integer.parseInt(tokens[i]);
                        }
                        int res = new Solution().maxProfit(prices);
                        System.out.println(res);
                    }
                }
                """
        ));

        templatesBySlug.put("best-time-to-buy-and-sell-stock", map);
    }

    private void registerMaxSubarray() {
        Map<SupportedLanguage, ProblemTemplate> map = new HashMap<>();

        // JAVA
        map.put(SupportedLanguage.JAVA, new ProblemTemplate(
                """
                class Solution {
                    public int maxSubArray(int[] nums) {
                        // Write your solution here
                        return 0;
                    }
                }
                """,
                """
                import java.util.*;

                {{USER_CODE}}

                class MainDriver {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (!sc.hasNextLine()) return;
                        String line = sc.nextLine().trim().replace("[", "").replace("]", "").trim();
                        String[] tokens = line.split("\\\\s*,\\\\s*|\\\\s+");
                        int[] nums = new int[tokens.length];
                        for (int i = 0; i < tokens.length; i++) {
                            nums[i] = Integer.parseInt(tokens[i]);
                        }
                        int res = new Solution().maxSubArray(nums);
                        System.out.println(res);
                    }
                }
                """
        ));

        templatesBySlug.put("maximum-subarray", map);
    }
}
