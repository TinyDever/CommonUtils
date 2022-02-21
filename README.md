# CommonUtils
Common utils for you to use easily.
Just add to your project what you need.

```
Usage:　
Java/RotatedLinkedList/RotatedLinkedList.java
```
```
RotatedLinkedList<Integer> strings = new RotatedLinkedList<>();
Integer[] integers = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
Collections.addAll(strings, integers);

strings.rotateToFirst(4);
for (Integer string : strings) {
    System.out.println(string);
}
```
```
Output:
 4 5 6 7 8 9 0 1 2 3
```
