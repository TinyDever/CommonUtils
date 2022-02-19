# CommonUtil
Common utils for you to use easily

```
Usage:　
Java/RotatedLinkedList/RotatedLinkedList.java
```
        int a = 0xC0;
        int c = 0xd0;
        System.out.println(Integer.toBinaryString(a));
        System.out.println(Integer.toBinaryString(c));
        System.out.println(Integer.toBinaryString(a | c));
        RotatedLinkedList<Integer> strings = new RotatedLinkedList<>();
        Integer[] integers = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        Collections.addAll(strings, integers);

        strings.rotateToFirst(4);
        for (Integer string : strings) {
            System.out.println(string);
        }
```
Output:
 4 5 6 7 8 9 0 1 2 3
```
