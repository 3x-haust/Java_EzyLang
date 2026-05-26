# EzyLang

EzyLang은 코틀린, 파이썬, 타입스크립트 등을 참고하여 만든 프로그래밍 언어입니다.

```
@memo
func fib(n: number): number {
    if (n <= 1) return n
    return fib(n - 1) + fib(n - 2)
}

println("fib(50) = ${fib(50)}")
```

## 실행

```bash
java -jar ezylang-3.0.0.jar <파일명>.ezy

java -jar ezylang-3.0.0.jar test <파일명>.ezy
```

## 빌드

```bash
./gradlew jar
```

---

## 문법

### 변수와 상수

```
name: string = "EzyLang"
age: number = 25
pi: number = 3.14
isActive: boolean = true

age = 26

$MAX_SIZE: number = 100
```

| 타입 | 설명 | 예시 |
|------|------|------|
| `number` | 숫자 (정수, 실수) | `42`, `3.14` |
| `string` | 문자열 (2글자 이상) | `"hello"` |
| `char` | 문자 (1글자) | `"A"` |
| `boolean` | 참/거짓 | `true`, `false` |
| `void` | 반환값 없음 | 함수 반환 타입 |
| `null` | 널 | `null` |

### 범위 제한 타입

```
age: number(0..150) = 25
age = 200  // 에러: Value 200.0 is out of range 0.0..150.0

grade: char("A", "B", "C", "D", "F") = "A"
grade = "Z"  // 에러: Value 'Z' is not allowed
```

### 연산자

```
a + b      a - b      a * b      a / b      a % b

x += 5     x -= 3     x *= 2     x /= 4     x %= 3

x++        x--        ++x        --x

a == b     a != b     a < b      a > b      a <= b     a >= b

a && b     a || b     !a

-x         +x
```

#### 연쇄 비교

```
if (10 < age < 30) { ... }
if (0 <= score <= 100) { ... }
```

### 문자열

```
name: string = "World"
println("Hello, ${name}!")
println("1 + 2 = ${1 + 2}")
println("result = ${math.sqrt(144)}")

text: string = "Hello, World!"
text.length()
text.charAt(0)
text.repeat(2)
text.split(", ")
```

### 출력

```
print("출력")
println("출력")
```

### 조건문

```
if (score >= 90) {
    println("A")
} else if (score >= 80) {
    println("B")
} else {
    println("F")
}

if (x > 5) break
if (flag) println("true")
```

### 반복문

```
for (i: number in 1 .. 10) {
    println(i)
}

for (i: number in 0 .. 10 .. 2) {
    println(i)
}

names: string[] = ["Alice", "Bob", "Charlie"]
for (name: string in names) {
    println(name)
}

while (count > 0) {
    count -= 1
}

for (i: number in 1 .. 100) {
    if (i % 2 == 0) continue
    if (i > 10) break
    println(i)
}
```

### 배열

```
numbers: number[] = [1, 2, 3, 4, 5]
names: string[] = ["Alice", "Bob"]

println(numbers[0])
numbers[0] = 99
```

| 메서드 | 설명 |
|--------|------|
| `length()` | 배열 길이 |
| `contains(elem)` | 포함 여부 |
| `indexOf(elem)` | 요소 위치 |
| `lastIndexOf(elem)` | 마지막 요소 위치 |
| `isEmpty()` | 비어있는지 확인 |
| `isNotEmpty()` | 비어있지 않은지 확인 |
| `sort()` | 정렬 |
| `reverse()` | 역순 |
| `shuffle()` | 무작위 섞기 |
| `remove(elem)` | 요소 제거 |
| `removeAt(index)` | 인덱스로 제거 |
| `clear()` | 전체 제거 |
| `addAll(arr)` | 배열 합치기 |
| `join(separator)` | 문자열로 합치기 |

### 함수

```
func greet(name: string): void {
    println("안녕하세요, ${name}!")
}

func add(a: number, b: number): number {
    return a + b
}

func factorial(n: number): number {
    if (n <= 1) return 1
    return n * factorial(n - 1)
}
```

### 메모이제이션

`@memo` 데코레이터를 함수에 붙이면 계산 결과를 캐시합니다.

```
@memo
func fib(n: number): number {
    if (n <= 1) return n
    return fib(n - 1) + fib(n - 2)
}

println(fib(50))
```

### switch

```
switch (day) {
    case 1: {
        println("월요일")
        break
    }
    case 2: {
        println("화요일")
        break
    }
    default:
        println("기타")
}

switch (grade) {
    case "A" -> println("우수")
    case "B" -> println("양호")
    default:
        println("기타")
}
```

### 타입 체크 / 캐스팅

```
x: number = 42
println(x is number)
println(x is string)

s: string = "123"
num: number = s as number
println(num + 7)
```

### 모듈

#### 네임스페이스 import

```
import math

println(math.sqrt(144))
println(math.PI)
```

#### 글로벌 import

```
from math import *
println(sqrt(144))

from math import sqrt, $PI
println(sqrt(144))
println(PI)
```

#### 사용자 모듈

```
// mylib.ezy
func add(a: number, b: number): number {
    return a + b
}
$PI: number = 3.14159
```

```
from mylib import add, $PI
println(add(3, 4))
println(PI)
```

### 주석

```
// 한 줄 주석

/*
여러 줄 주석
*/
```

### 클래스

```
class Person(name: string, age: number) {
    func greet(): string {
        return "안녕하세요, ${self.name}입니다."
    }
}

person: Person = new Person("김철수", 25)
println(person.greet())
println(person.name)
```

#### 기본값

```
class Config(host: string = "localhost", port: number = 8080) {
}

config: Config = new Config()
println(config.host)  // localhost
```

### 상속

`:` 뒤에 부모 클래스를 지정합니다. `@override`로 메서드를 재정의하고, `parent`로 부모 메서드를 호출합니다.

```
class Animal(name: string, age: number) {
    func info(): string {
        return "${self.name} (${self.age}살)"
    }
}

class Dog(name: string, age: number, breed: string = "믹스")
    : Animal(name, age) {

    @override
    func speak(): string {
        return "${self.name}: 멍멍!"
    }

    func fullInfo(): string {
        return "${parent.info()} - ${self.breed}"
    }
}

dog: Dog = new Dog("바둑이", 3)
println(dog is Animal)  // true
```

### 인터페이스

```
interface Displayable {
    func display(): string
}

interface Saveable {
    func save(): string
}

class Document(title: string, content: string) : Displayable, Saveable {
    func display(): string {
        return "[문서] ${self.title}"
    }
    func save(): string {
        return "${self.title} 저장 완료"
    }
}

doc: Document = new Document("보고서", "내용")
println(doc is Displayable)  // true
```

상속과 인터페이스를 동시에 사용할 수 있습니다:

```
class SpecialDoc(title: string, content: string)
    : Document(title, content), Displayable, Saveable {
}
```

### 데코레이터

#### 클래스 데코레이터

| 데코레이터 | 설명 |
|------------|------|
| `@getter` | 필드별 `getName()` 메서드 자동 생성 |
| `@setter` | 필드별 `setName(value)` 메서드 자동 생성 |
| `@data` | `@getter` + `@setter` + `toString()` + `equals()` + `copy()` |

```
@data
class Point(x: number, y: number) {
}

p1: Point = new Point(10, 20)
p2: Point = new Point(10, 20)
println(p1)           // Point(x=10, y=20)
println(p1 == p2)     // true
p3: Point = p1.copy()
```

#### 함수 데코레이터

| 데코레이터 | 설명 |
|------------|------|
| `@memo` | 계산 결과 캐싱 (메모이제이션) |
| `@test("이름")` | 테스트 함수 선언 (`test` 모드에서 실행) |
| `@log` | 함수 호출/반환값 로그 출력 |
| `@deprecated("메시지")` | 호출 시 경고 메시지 출력 |
| `@guard("param", "조건")` | 매개변수 유효성 검증 |

```
@log
func add(a: number, b: number): number {
    return a + b
}
// [LOG] add(3, 4) → 7

@deprecated("newFunc를 사용하세요")
func oldFunc(): void { }
// [WARNING] oldFunc is deprecated: newFunc를 사용하세요

@guard("n", "n >= 0")
func factorial(n: number): number {
    if (n <= 1) return 1
    return n * factorial(n - 1)
}
```

#### 커스텀 데코레이터

`decorator` 키워드로 직접 데코레이터를 정의할 수 있습니다. `call()`로 원본 함수를 호출합니다.

```
decorator counter() {
    println("[counter] 함수 실행")
    result: any = call()
    println("[counter] 실행 완료")
    return result
}

@counter
func sayHello(name: string): string {
    return "Hello, ${name}!"
}
```

### entry 블록

`entry`는 프로그램의 진입점입니다. 직접 실행하면 `entry` 블록이 실행되고, 모듈로 import하면 무시됩니다.

```
func greet(name: string): string {
    return "안녕, ${name}!"
}

entry {
    println(greet("EzyLang"))
}
```

- **직접 실행**: 탑레벨 코드 + `entry` 블록 모두 실행
- **모듈로 import**: 탑레벨 코드만 실행, `entry` 블록은 무시

Python의 `if __name__ == "__main__":` 와 같은 역할입니다.

### 블록 스코프

```
x: number = 10
{
    y: number = 20
    println(y)
    println(x)
}
```

### 내장 테스트

`@test("이름")` 데코레이터를 함수에 붙여 테스트를 작성합니다.

```
func add(a: number, b: number): number {
    return a + b
}

@test("덧셈 테스트")
func testAdd(): void {
    assert add(1, 2) == 3
    assert add(-1, 1) == 0
}
```

```bash
java -jar ezylang-3.0.0.jar test app.ezy
# [PASS] 덧셈 테스트
# === 1 passed, 0 failed ===
```

---

## 내장 모듈

### math

```
import math

math.sqrt(144)        math.abs(-42)         math.pow(2, 10)
math.min(3, 7)        math.max(3, 7)
math.floor(3.7)       math.ceil(3.2)        math.round(3.5)
math.sin(x)           math.cos(x)           math.tan(x)
math.asin(x)          math.acos(x)          math.atan(x)
math.log(x)           math.log10(x)
math.random()         math.toRadians(deg)   math.toDegrees(rad)
math.PI               math.E
```

### str

```
import str

str.toUpperCase(s)    str.toLowerCase(s)    str.trim(s)
str.startsWith(s, p)  str.endsWith(s, p)    str.strContains(s, sub)
str.strIndexOf(s, sub) str.strLength(s)
str.substring(s, start, end)                 str.replace(s, old, new)
str.reverse(s)        str.padLeft(s, len, ch) str.padRight(s, len, ch)
str.format(num, decimals)
```

### arr

```
import arr

arr.range(1, 10)      arr.range(0, 10, 2)   arr.fill(5, 0)
arr.sum(nums)         arr.avg(nums)
arr.arrMin(nums)      arr.arrMax(nums)
arr.slice(nums, 1, 3) arr.count(nums, 2)
```

### io

```
from io import *

content: string = readFile("data.txt")
writeFile("output.txt", "Hello!")
appendFile("output.txt", "\nWorld!")
lines: any = readLines("data.txt")

readLine("이름: ")              // 사용자 입력
fileExists("data.txt")          // 파일 존재 확인
deleteFile("temp.txt")           // 파일 삭제
fileSize("data.txt")             // 파일 크기 (bytes)
listDir(".")                     // 디렉토리 목록
mkdir("new_dir")                 // 디렉토리 생성
isDir("path")                    // 디렉토리 여부
isFile("path")                   // 파일 여부
```

### os

```
from os import *

env("HOME")                      // 환경변수
env("NO_VAR", "기본값")          // 기본값 지정
exec("ls -la")                   // 프로세스 실행
cwd()                            // 현재 디렉토리
homedir()                        // 홈 디렉토리
platform()                       // OS 이름
exit(0)                          // 프로세스 종료

pathJoin("/usr", "local", "bin") // 경로 결합
pathDir("/usr/local/bin")        // 디렉토리 부분
pathFile("/usr/local/file.txt")  // 파일명 부분
pathExt("file.txt")              // 확장자

SEP                              // 경로 구분자
EOL                              // 줄바꿈 문자
```

### time

```
from time import *

now()                            // 현재 타임스탬프 (ms)
sleep(1000)                      // 대기 (ms)
format(now())                    // "2026-04-09 15:30:00"
format(now(), "yyyy/MM/dd")      // "2026/04/09"

year()  month()  day()           // 현재 날짜
hour()  minute()  second()       // 현재 시간
toSeconds(5000)                  // ms → 초
toMillis(3)                      // 초 → ms
```

### json

```
from json import *

data: any = parse("{\"name\": \"김철수\", \"age\": 25}")
get(data, "name")                // "김철수"
set(data, "email", "a@b.com")   // 필드 추가
hasKey(data, "name")             // true
keys(data)                       // 키 목록
values(data)                     // 값 목록
remove(data, "age")              // 키 삭제
stringify(data)                  // JSON 문자열로 변환
newObject()                      // 빈 객체 생성
```

### http

```
from http import *

func handleHello(body: string, query: string): string {
    return "{\"message\": \"Hello!\"}"
}

routeGet("/api/hello", "handleHello")
routePost("/api/echo", "handleEcho")
serve(8080)
```

| 함수 | 설명 |
|------|------|
| `routeGet(path, handler)` | GET 라우트 등록 |
| `routePost(path, handler)` | POST 라우트 등록 |
| `routePut(path, handler)` | PUT 라우트 등록 |
| `routeDelete(path, handler)` | DELETE 라우트 등록 |
| `serve(port)` | HTTP 서버 시작 |

### net

```
from net import *

response: string = httpGet("https://api.example.com/data")
httpPost("https://api.example.com/data", "{\"key\": \"value\"}")
httpPut(url, body)
httpDelete(url)
status: number = httpStatus(url)
```

---

## 예제

`src/main/resources/examples/` 디렉토리에 예제 파일이 있습니다:

| 파일 | 내용 |
|------|------|
| `01_hello_world.ezy` | 기본 출력 |
| `02_variables.ezy` | 변수, 상수, 타입 |
| `03_arithmetic.ezy` | 산술, 복합 대입 연산자 |
| `04_string_interpolation.ezy` | 문자열 보간, 이스케이프 |
| `05_if_else.ezy` | 조건문, 논리/비교 연산자 |
| `06_loops.ezy` | for, while, break, continue |
| `07_arrays.ezy` | 배열 선언, 순회, 메서드 |
| `08_functions.ezy` | 함수, 재귀 |
| `09_switch.ezy` | switch 콜론/화살표 스타일 |
| `10_increment_decrement.ezy` | 증감 연산자 |
| `11_type_check_cast.ezy` | is, as |
| `12_string_methods.ezy` | 문자열 메서드 |
| `13_array_methods.ezy` | 배열 메서드 |
| `14_comments.ezy` | 주석 |
| `15_import_module.ezy` | 사용자 모듈 import |
| `16_nested_loops.ezy` | 중첩 반복 |
| `17_bubble_sort.ezy` | 버블 정렬 |
| `18_calculator.ezy` | 계산기 |
| `19_scope.ezy` | 블록 스코프 |
| `20_unary_operators.ezy` | 단항 연산자 |
| `21_chained_comparison.ezy` | 연쇄 비교 |
| `22_constrained_types.ezy` | 범위 제한 타입 |
| `23_test_block.ezy` | @test 데코레이터 |
| `24_memoization.ezy` | @memo 데코레이터 |
| `25_math_module.ezy` | math 모듈 |
| `26_str_module.ezy` | str 모듈 |
| `27_arr_module.ezy` | arr 모듈 |
| `28_namespace_import.ezy` | 네임스페이스 import |
| `29_class_basic.ezy` | 클래스, 생성자, 기본값 |
| `30_inheritance.ezy` | 상속, override, parent |
| `31_interface.ezy` | 인터페이스, 다중 구현 |
| `32_decorators.ezy` | @getter, @setter, @data |
| `33_function_decorators.ezy` | @log, @deprecated, @guard, @memo |
| `34_custom_decorator.ezy` | 커스텀 데코레이터 |
| `35_entry_block.ezy` | entry 진입점 |
| `36_io_module.ezy` | io 모듈 (파일/입력) |
| `37_os_module.ezy` | os 모듈 (시스템) |
| `38_time_module.ezy` | time 모듈 (시간) |
| `39_json_module.ezy` | json 모듈 (JSON) |
| `40_http_server.ezy` | http 모듈 (서버) |
| `41_net_module.ezy` | net 모듈 (HTTP 클라이언트) |
