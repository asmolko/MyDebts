MyDebts
==========

[![Build Status](https://travis-ci.org/asmolko/MyDebts.svg?branch=master)](https://travis-ci.org/asmolko/MyDebts)

[![Coverage Status](https://coveralls.io/repos/github/asmolko/MyDebts/badge.svg?branch=master)](https://coveralls.io/github/asmolko/MyDebts?branch=master)

A collaborative effort to implement debt ralaxation software after our
frequent dinner-split parties :)

Features
----------

- Simple centralized intuitive debt tracking
- Notifying other person about its debt against you
- Debt relaxation (mutual settlements in a parties)

How does it work
-----------------

1. When app starts, it requests info about you (id, display name and avatar) and your contacts from G+
2. Once this is done, it requests a list of debts from a server that are related to you
3. You may do whatever you want with this data (WIP):
  1. approve debts that were created against you
  2. create new debts against your contacts
  3. modify/delete existing debts and re-approve actions
4. Once server has got conflicting debts it will try to automatically relax these 
  (say, Alice owes Bob $5. Bob owes Eva $3 and Eva owes Alice 10$ - the server will
  simplify this to Alice --$2--> Bob --$0--> Eva --$7--> Alice)
5. ???
6. PROFIT!
