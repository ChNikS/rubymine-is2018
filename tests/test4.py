#logic operations support test
if 3 > 4 and 5 < 10:
    pass

if not(3 > 4) or 5 > 10:
    pass

if 12 > 1 or 12 == 12:
    pass

if 12 > 1 or 12 != 12:
    pass

if 5 < 1 or 12 != 12:
    pass

if 12 > 1 and 12 == 12:
    pass

if 5 < 1 and 5 == 5:
    pass

if not(5 < 1):
    pass

if not(5 > 1):
    pass

if not(5 > 1) and 5 != 5:
    pass

if not(5 > 1) or 5 == 5:
    pass

if not(5 > 1) and 5 != 5 or 7 > 5:
    pass