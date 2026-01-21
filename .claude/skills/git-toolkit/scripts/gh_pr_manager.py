#!/usr/bin/env python3
# .claude/skills/git-toolkit/scripts/gh_pr_manager.py
# GitHub PR 생성 및 관리 스크립트

import subprocess
import json
import sys
import argparse
from typing import Optional, List, Dict, Any, Tuple


def run_gh_command(args: List[str]) -> Tuple[int, str, str]:
    """gh 명령어 실행"""
    try:
        result = subprocess.run(
            ['gh'] + args,
            capture_output=True,
            text=True,
            timeout=60
        )
        return result.returncode, result.stdout.strip(), result.stderr.strip()
    except FileNotFoundError:
        return 1, '', 'GitHub CLI (gh) is not installed'
    except subprocess.TimeoutExpired:
        return 1, '', 'Command timed out'


def check_gh_auth() -> bool:
    """gh 인증 상태 확인"""
    code, _, _ = run_gh_command(['auth', 'status'])
    return code == 0


def create_pr(
    title: str,
    body: str,
    base: str = 'main',
    labels: Optional[List[str]] = None,
    draft: bool = False,
    reviewers: Optional[List[str]] = None
) -> Dict[str, Any]:
    """PR 생성"""
    if not check_gh_auth():
        return {
            'success': False,
            'error': 'Not authenticated with GitHub CLI. Run: gh auth login'
        }

    args = ['pr', 'create', '--title', title, '--body', body, '--base', base]

    if labels:
        for label in labels:
            args.extend(['--label', label])

    if reviewers:
        for reviewer in reviewers:
            args.extend(['--reviewer', reviewer])

    if draft:
        args.append('--draft')

    print(f"🚀 Creating PR: {title}")
    print(f"   Base branch: {base}")

    code, stdout, stderr = run_gh_command(args)

    if code != 0:
        return {
            'success': False,
            'error': stderr or 'Failed to create PR',
            'command': f"gh {' '.join(args)}"
        }

    print(f"✅ PR created: {stdout}")

    return {
        'success': True,
        'pr_url': stdout,
        'title': title,
        'base': base,
        'labels': labels or [],
        'draft': draft
    }


def get_pr_status(pr_number: Optional[int] = None) -> Dict[str, Any]:
    """PR 상태 조회"""
    args = ['pr', 'view']
    if pr_number:
        args.append(str(pr_number))
    args.extend(['--json', 'number,title,state,mergeable,reviews,statusCheckRollup,headRefName,baseRefName'])

    code, stdout, stderr = run_gh_command(args)

    if code != 0:
        return {
            'success': False,
            'error': stderr or 'Failed to get PR status'
        }

    try:
        data = json.loads(stdout)
        return {
            'success': True,
            'data': data
        }
    except json.JSONDecodeError:
        return {
            'success': False,
            'error': 'Failed to parse PR data'
        }


def list_prs(state: str = 'open', limit: int = 30) -> Dict[str, Any]:
    """PR 목록 조회"""
    args = ['pr', 'list', '--state', state, '--limit', str(limit),
            '--json', 'number,title,state,author,createdAt,headRefName']

    code, stdout, stderr = run_gh_command(args)

    if code != 0:
        return {
            'success': False,
            'error': stderr or 'Failed to list PRs'
        }

    try:
        prs = json.loads(stdout)
        return {
            'success': True,
            'count': len(prs),
            'prs': prs
        }
    except json.JSONDecodeError:
        return {
            'success': False,
            'error': 'Failed to parse PR list'
        }


def add_labels(pr_number: int, labels: List[str]) -> Dict[str, Any]:
    """PR에 레이블 추가"""
    args = ['pr', 'edit', str(pr_number)]
    for label in labels:
        args.extend(['--add-label', label])

    code, stdout, stderr = run_gh_command(args)

    if code != 0:
        return {
            'success': False,
            'error': stderr or 'Failed to add labels'
        }

    return {
        'success': True,
        'pr_number': pr_number,
        'added_labels': labels
    }


def add_reviewers(pr_number: int, reviewers: List[str]) -> Dict[str, Any]:
    """PR에 리뷰어 추가"""
    args = ['pr', 'edit', str(pr_number)]
    for reviewer in reviewers:
        args.extend(['--add-reviewer', reviewer])

    code, stdout, stderr = run_gh_command(args)

    if code != 0:
        return {
            'success': False,
            'error': stderr or 'Failed to add reviewers'
        }

    return {
        'success': True,
        'pr_number': pr_number,
        'added_reviewers': reviewers
    }


def merge_pr(
    pr_number: int,
    method: str = 'squash',
    delete_branch: bool = True,
    admin: bool = False
) -> Dict[str, Any]:
    """PR 머지"""
    args = ['pr', 'merge', str(pr_number), f'--{method}']

    if delete_branch:
        args.append('--delete-branch')

    if admin:
        args.append('--admin')

    print(f"🔀 Merging PR #{pr_number} using {method}...")

    code, stdout, stderr = run_gh_command(args)

    if code != 0:
        return {
            'success': False,
            'error': stderr or 'Failed to merge PR'
        }

    print(f"✅ PR #{pr_number} merged successfully!")

    return {
        'success': True,
        'pr_number': pr_number,
        'merge_method': method,
        'branch_deleted': delete_branch
    }


def close_pr(pr_number: int, comment: Optional[str] = None) -> Dict[str, Any]:
    """PR 닫기"""
    args = ['pr', 'close', str(pr_number)]

    if comment:
        args.extend(['--comment', comment])

    code, stdout, stderr = run_gh_command(args)

    if code != 0:
        return {
            'success': False,
            'error': stderr or 'Failed to close PR'
        }

    return {
        'success': True,
        'pr_number': pr_number,
        'closed': True
    }


def main():
    parser = argparse.ArgumentParser(
        description='GitHub PR Manager for NEXT-UP Project',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Create a new PR
  %(prog)s create --title "feat: Add player API" --body "## Changes\\n- Added Player entity" --base main

  # Get PR status
  %(prog)s status --number 1

  # List open PRs
  %(prog)s list --state open

  # Merge a PR
  %(prog)s merge --number 1 --method squash
        """
    )

    subparsers = parser.add_subparsers(dest='command', required=True)

    # create 명령어
    create_parser = subparsers.add_parser('create', help='Create a new PR')
    create_parser.add_argument('--title', required=True, help='PR title')
    create_parser.add_argument('--body', required=True, help='PR body (supports markdown)')
    create_parser.add_argument('--base', default='main', help='Base branch (default: main)')
    create_parser.add_argument('--label', action='append', dest='labels', help='Labels (can be used multiple times)')
    create_parser.add_argument('--reviewer', action='append', dest='reviewers', help='Reviewers (can be used multiple times)')
    create_parser.add_argument('--draft', action='store_true', help='Create as draft PR')

    # status 명령어
    status_parser = subparsers.add_parser('status', help='Get PR status')
    status_parser.add_argument('--number', type=int, help='PR number (default: current branch PR)')

    # list 명령어
    list_parser = subparsers.add_parser('list', help='List PRs')
    list_parser.add_argument('--state', default='open', choices=['open', 'closed', 'merged', 'all'])
    list_parser.add_argument('--limit', type=int, default=30, help='Maximum number of PRs to list')

    # label 명령어
    label_parser = subparsers.add_parser('label', help='Add labels to PR')
    label_parser.add_argument('--number', type=int, required=True, help='PR number')
    label_parser.add_argument('--add', action='append', required=True, dest='labels', help='Labels to add')

    # reviewer 명령어
    reviewer_parser = subparsers.add_parser('reviewer', help='Add reviewers to PR')
    reviewer_parser.add_argument('--number', type=int, required=True, help='PR number')
    reviewer_parser.add_argument('--add', action='append', required=True, dest='reviewers', help='Reviewers to add')

    # merge 명령어
    merge_parser = subparsers.add_parser('merge', help='Merge PR')
    merge_parser.add_argument('--number', type=int, required=True, help='PR number')
    merge_parser.add_argument('--method', default='squash', choices=['merge', 'squash', 'rebase'])
    merge_parser.add_argument('--no-delete-branch', action='store_true', help='Do not delete branch after merge')
    merge_parser.add_argument('--admin', action='store_true', help='Use admin privileges to merge')

    # close 명령어
    close_parser = subparsers.add_parser('close', help='Close PR without merging')
    close_parser.add_argument('--number', type=int, required=True, help='PR number')
    close_parser.add_argument('--comment', help='Comment to add when closing')

    args = parser.parse_args()

    result = {}

    if args.command == 'create':
        result = create_pr(
            title=args.title,
            body=args.body,
            base=args.base,
            labels=args.labels,
            draft=args.draft,
            reviewers=args.reviewers
        )
    elif args.command == 'status':
        result = get_pr_status(args.number)
    elif args.command == 'list':
        result = list_prs(args.state, args.limit)
    elif args.command == 'label':
        result = add_labels(args.number, args.labels)
    elif args.command == 'reviewer':
        result = add_reviewers(args.number, args.reviewers)
    elif args.command == 'merge':
        result = merge_pr(args.number, args.method, not args.no_delete_branch, args.admin)
    elif args.command == 'close':
        result = close_pr(args.number, args.comment)

    print("\n--- JSON Result ---")
    print(json.dumps(result, indent=2, ensure_ascii=False))

    sys.exit(0 if result.get('success', False) else 1)


if __name__ == '__main__':
    main()
