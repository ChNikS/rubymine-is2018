#some extra supported expressions
def getBool():
    return False

b = 2
if getBool() and 1>4:
    pass
if getBool() or 2>1:
    pass

if getBool() or (1 == 1 and 5<1):
    pass

#show alert message for part of possible expressions

if 5>20 or getBool() or (1 == 1 and 5<1):
    pass